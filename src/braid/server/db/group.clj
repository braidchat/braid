(ns braid.server.db.group
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [braid.server.db.common :refer :all]
            [braid.server.db.user :as user]))

(defn group-exists?
  [conn group-name]
  (some? (d/pull (d/db conn) '[:group/id] [:group/name group-name])))

(defn create-group!
  [conn {:keys [name id]}]
  (->> {:group/id id
        :group/name name}
       (create-entity! conn)
       db->group))

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

(defn group-set!
  "Set a key to a value for the group's settings  This will throw if
  settings are changed in between reading & setting"
  [conn group-id k v]
  (let [old-prefs (-> (d/pull (d/db conn) [:group/settings] [:group/id group-id])
                      :group/settings)
        new-prefs (-> ((fnil edn/read-string "{}") old-prefs)
                      (assoc k v)
                      pr-str)]
    @(d/transact conn [[:db.fn/cas [:group/id group-id]
                        :group/settings old-prefs new-prefs]])))

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

(defn user-add-to-group! [conn user-id group-id]
  @(d/transact conn [[:db/add [:group/id group-id]
                      :group/user [:user/id user-id]]]))

(defn user-leave-group! [conn user-id group-id]
  (let [unsub-txn (->> (d/q '[:find [?t ...]
                              :in $ ?user-id ?group-id
                              :where
                              [?u :user/id ?user-id]
                              [?g :group/id ?group-id]
                              [?t :thread/group ?g]
                              [?u :user/subscribed-thread ?t]]
                            (d/db conn) user-id group-id)
                       (map (fn [t]
                              [:db/retract [:user/id user-id]
                               :user/subscribed-thread t])))
        unmention-txn (->> (d/q '[:find [?t ...]
                                  :in $ ?user-id ?group-id
                                  :where
                                  [?u :user/id ?user-id]
                                  [?g :group/id ?group-id]
                                  [?t :thread/group ?g]
                                  [?t :thread/mentioned ?u]]
                                (d/db conn) user-id group-id)
                           (map (fn [t]
                                  [:db/retract t
                                   :thread/mentioned [:user/id user-id]])))]
    (when-let [order (user/user-get-preference conn user-id :groups-order)]
      (user/user-set-preference!
        conn
        user-id :groups-order
        (into [] (remove (partial = group-id)) order)))
    @(d/transact conn (concat
                        [[:db/retract [:group/id group-id]
                          :group/user [:user/id user-id]]]
                        unsub-txn
                        unmention-txn))))

(defn user-make-group-admin! [conn user-id group-id]
  @(d/transact conn [[:db/add [:group/id group-id]
                      :group/user [:user/id user-id]]
                     [:db/add [:group/id group-id]
                      :group/admins [:user/id user-id]]]))

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

(defn user-subscribe-to-group-tags!
  "Subscribe the user to all current tags in the group"
  [conn user-id group-id]
  (->> (d/q '[:find ?tag
              :in $ ?group-id
              :where
              [?tag :tag/group ?g]
              [?g :group/id ?group-id]]
            (d/db conn) group-id)
       (map (fn [[tag]]
              [:db/add [:user/id user-id]
               :user/subscribed-tag tag]))
       (d/transact conn)
       deref))

