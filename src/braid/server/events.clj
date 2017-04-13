(ns braid.server.events
  (:require
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.sync-helpers :as sync-helpers]))

(defn add-user-to-recent-threads-in-group!
  [user-id group-id]
  (doseq [t (db/recent-threads {:user-id user-id :group-id group-id
                                :num-threads 5})]
    (db/user-show-thread! user-id (t :id))))

(defn user-join-group!
  [user-id group-id]
  (db/run-txns!
    (concat
      (group/user-add-to-group-txn user-id group-id)
      (group/user-subscribe-to-group-tags-txn user-id group-id)
      (group/add-user-to-recent-threads-in-group-txn user-id group-id)))
  (sync-helpers/broadcast-group-change group-id [:braid.client/new-user (db/user-by-id user-id)]))

(defn register-user!
  [email group-id]
  (let [user (db/create-user! {:id (db/uuid)
                               :email email})]
    (user-join-group! (user :id) group-id)
    (user :id)))

