(ns braid.server.db.group
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [braid.server.db.common :refer :all]
            [braid.server.db.user :as user]))

;; Queries

(defn group-exists?
  [conn group-name]
  (some? (d/pull (d/db conn) '[:group/id] [:group/name group-name])))

(defn group-by-id
  [conn group-id]
  (-> (d/pull (d/db conn) group-pull-pattern [:group/id group-id])
      db->group))

(defn group-users
  [conn group-id]
  (->> (d/q '[:find (pull ?u pull-pattern)
              :in $ ?group-id pull-pattern
              :where
              [?g :group/id ?group-id]
              [?g :group/user ?u]]
            (d/db conn)
            group-id
            user-pull-pattern)
       (map (comp db->user first))
       set))

(defn group-settings
  [conn group-id]
  (->> (d/pull (d/db conn) [:group/settings] [:group/id group-id])
       :group/settings
       ((fnil edn/read-string "{}"))))

(defn public-group-with-name
  [conn group-name]
  (when-let [group (-> (d/pull (d/db conn) group-pull-pattern
                               [:group/name group-name])
                       db->group)]
    (when (:public? (group-settings conn (group :id)))
      group)))

(defn group-tags
  [conn group-id]
  (->> (d/q '[:find (pull ?t [:tag/id
                              :tag/name
                              :tag/description
                              {:tag/group [:group/id :group/name]}])
              :in $ ?group-id
              :where
              [?g :group/id ?group-id]
              [?t :tag/group ?g]]
            (d/db conn) group-id)
       (map (comp db->tag first))))

(defn user-groups [conn user-id]
  (->> (d/q '[:find [?g ...]
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]]
            (d/db conn)
            user-id)
       (d/pull-many (d/db conn) group-pull-pattern)
       (map (comp #(dissoc % :users) db->group))
       set))

(defn user-in-group?
  [conn user-id group-id]
  (seq (d/q '[:find ?g
              :in $ ?user-id ?group-id
              :where
              [?u :user/id ?user-id]
              [?g :group/id ?group-id]
              [?g :group/user ?u]]
            (d/db conn)
            user-id group-id)))

(defn user-is-group-admin?
  [conn user-id group-id]
  (some?
    (d/q '[:find ?u .
           :in $ ?user-id ?group-id
           :where
           [?g :group/id ?group-id]
           [?u :user/id ?user-id]
           [?g :group/admins ?u]]
         (d/db conn) user-id group-id)))

;; Transactions

(defn create-group!
  [conn {:keys [name id]}]
  (->> {:group/id id
        :group/name name}
       (create-entity! conn)
       db->group))

(defn group-set-txn
  "Set a key to a value for the group's settings  This will throw if
  settings are changed in between reading & setting"
  [conn group-id k v]
  (let [old-prefs (-> (d/pull (d/db conn) [:group/settings] [:group/id group-id])
                      :group/settings)
        new-prefs (-> ((fnil edn/read-string "{}") old-prefs)
                      (assoc k v)
                      pr-str)]
    [[:db.fn/cas [:group/id group-id] :group/settings old-prefs new-prefs]]))

(defn user-add-to-group-txn [conn user-id group-id]
  [[:db/add [:group/id group-id] :group/user [:user/id user-id]]])

(defn user-leave-group-txn
  [conn user-id group-id]
  (concat
    [[:db/retract [:group/id group-id] :group/user [:user/id user-id]]]
    ; unsubscribe from threads
    (->>
      (d/q '[:find [?t ...]
             :in $ ?user-id ?group-id
             :where
             [?u :user/id ?user-id]
             [?g :group/id ?group-id]
             [?t :thread/group ?g]
             [?u :user/subscribed-thread ?t]]
           (d/db conn) user-id group-id)
      (map (fn [t] [:db/retract [:user/id user-id] :user/subscribed-thread t])))
    ; remove mentions
    (->> (d/q '[:find [?t ...]
                :in $ ?user-id ?group-id
                :where
                [?u :user/id ?user-id]
                [?g :group/id ?group-id]
                [?t :thread/group ?g]
                [?t :thread/mentioned ?u]]
              (d/db conn) user-id group-id)
         (map (fn [t] [:db/retract t :thread/mentioned [:user/id user-id]])))
    ; remove group from custom sort order
    (if-let [order (user/user-get-preference conn user-id :groups-order)]
      (user/user-set-preference-txn
        conn user-id :groups-order (vec (remove (partial = group-id) order)))
      [])))

(defn user-make-group-admin-txn [conn user-id group-id]
  [[:db/add [:group/id group-id] :group/user [:user/id user-id]]
   [:db/add [:group/id group-id] :group/admins [:user/id user-id]]])

(defn user-subscribe-to-group-tags-txn
  "Subscribe the user to all current tags in the group"
  [conn user-id group-id]
  (->>
    (d/q '[:find ?tag
           :in $ ?group-id
           :where
           [?tag :tag/group ?g]
           [?g :group/id ?group-id]]
         (d/db conn) group-id)
    (map (fn [[tag]] [:db/add [:user/id user-id] :user/subscribed-tag tag]))))
