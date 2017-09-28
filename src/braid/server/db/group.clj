(ns braid.server.db.group
  (:require
    [clojure.edn :as edn]
    [datomic.api :as d]
    [braid.server.db :as db]
    [braid.server.db.common :refer [create-entity-txn db->group db->user db->tag
                                    group-pull-pattern user-pull-pattern]]
    [braid.server.db.thread :as thread]
    [braid.server.db.user :as user]))

(defn group-exists?
  [group-name]
  (some? (d/pull (db/db) '[:group/id] [:group/name group-name])))

(defn group-with-slug-exists?
  [slug]
  (boolean (first (d/q '[:find [?g]
                         :in $ ?slug
                         :where
                         [?g :group/slug ?slug]]
                       (db/db)
                       slug))))

(defn group-by-id
  [group-id]
  (-> (d/pull (db/db) group-pull-pattern [:group/id group-id])
      db->group))

(defn group-by-slug
  [group-slug]
  (when-let [g (d/pull (db/db) group-pull-pattern [:group/slug group-slug])]
    (db->group g)))

(defn group-users
  [group-id]
  (->> (d/q '[:find (pull ?u pull-pattern)
              :in $ ?group-id pull-pattern
              :where
              [?g :group/id ?group-id]
              [?g :group/user ?u]]
            (db/db)
            group-id
            user-pull-pattern)
       (map (comp db->user first))
       set))

(defn group-settings
  [group-id]
  (->> (d/pull (db/db) [:group/settings] [:group/id group-id])
       :group/settings
       ((fnil edn/read-string "{}"))))

(defn group-tags
  [group-id]
  (->> (d/q '[:find (pull ?t [:tag/id
                              :tag/name
                              :tag/description
                              {:tag/group [:group/id :group/name]}])
              :in $ ?group-id
              :where
              [?g :group/id ?group-id]
              [?t :tag/group ?g]]
            (db/db) group-id)
       (map (comp db->tag first))))

(defn user-groups
  [user-id]
  (->> (d/q '[:find [?g ...]
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]]
            (db/db)
            user-id)
       (d/pull-many (db/db) group-pull-pattern)
       (map (comp #(dissoc % :users) db->group))
       set))

(defn user-in-group?
  [user-id group-id]
  (-> (d/q '[:find ?g
             :in $ ?user-id ?group-id
             :where
             [?u :user/id ?user-id]
             [?g :group/id ?group-id]
             [?g :group/user ?u]]
           (db/db)
           user-id group-id)
      seq
      boolean))

(defn user-is-group-admin?
  [user-id group-id]
  (some?
    (d/q '[:find ?u .
           :in $ ?user-id ?group-id
           :where
           [?g :group/id ?group-id]
           [?u :user/id ?user-id]
           [?g :group/admins ?u]]
         (db/db) user-id group-id)))

;; Transactions

(defn create-group-txn
  [{:keys [name slug id]}]
  (create-entity-txn
    {:group/id id
     :group/slug slug
     :group/name name}
    db->group))

(defn group-set-txn
  "Set a key to a value for the group's settings  This will throw if
  settings are changed in between reading & setting"
  [group-id k v]
  (let [old-prefs (-> (d/pull (db/db) [:group/settings] [:group/id group-id])
                      :group/settings)
        new-prefs (-> ((fnil edn/read-string "{}") old-prefs)
                      (assoc k v)
                      pr-str)]
    [[:db.fn/cas [:group/id group-id] :group/settings old-prefs new-prefs]]))

(defn user-add-to-group-txn
  [user-id group-id]
  [[:db/add [:group/id group-id] :group/user [:user/id user-id]]])

(defn user-leave-group-txn
  [user-id group-id]
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
           (db/db) user-id group-id)
      (map (fn [t] [:db/retract [:user/id user-id] :user/subscribed-thread t])))
    ; remove mentions
    (->> (d/q '[:find [?t ...]
                :in $ ?user-id ?group-id
                :where
                [?u :user/id ?user-id]
                [?g :group/id ?group-id]
                [?t :thread/group ?g]
                [?t :thread/mentioned ?u]]
              (db/db) user-id group-id)
         (map (fn [t] [:db/retract t :thread/mentioned [:user/id user-id]])))
    ; remove group from custom sort order
    (if-let [order (user/user-get-preference user-id :groups-order)]
      (user/user-set-preference-txn
        user-id :groups-order (vec (remove (partial = group-id) order)))
      [])))

(defn user-make-group-admin-txn
  [user-id group-id]
  [[:db/add [:group/id group-id] :group/user [:user/id user-id]]
   [:db/add [:group/id group-id] :group/admins [:user/id user-id]]])

(defn user-subscribe-to-group-tags-txn
  "Subscribe the user to all current tags in the group"
  [user-id group-id]
  (->>
    (d/q '[:find ?tag
           :in $ ?group-id
           :where
           [?tag :tag/group ?g]
           [?g :group/id ?group-id]]
         (db/db) group-id)
    (map (fn [[tag]] [:db/add [:user/id user-id] :user/subscribed-tag tag]))))

(defn user-join-group-txn
  "Add a user to the given group, subscribe them to the group tags,
  and subscribe them to the five most recent threads in the group."
  [user-id group-id]
  (concat
    (user-add-to-group-txn user-id group-id)
    (user-subscribe-to-group-tags-txn user-id group-id)
    ; add user to recent threads in group
    (mapcat
      (fn [t] (thread/user-show-thread-txn user-id (t :id)))
      (thread/recent-threads {:user-id user-id
                              :group-id group-id
                              :num-threads 5}))))
