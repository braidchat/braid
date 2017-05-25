(ns braid.server.events
  (:require
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.db.thread :as thread]
    [braid.server.db.user :as user]
    [braid.server.sync-helpers :as sync-helpers]))

(defn add-user-to-recent-threads-in-group!
  [user-id group-id]
  (doseq [t (thread/recent-threads {:user-id user-id :group-id group-id
                                :num-threads 5})]
    (db/run-txns! (thread/user-show-thread-txn user-id (t :id)))))

(defn user-join-group!
  [user-id group-id]
  (db/run-txns!
    (concat
      (group/user-add-to-group-txn user-id group-id)
      (group/user-subscribe-to-group-tags-txn user-id group-id)
      (add-user-to-recent-threads-in-group! user-id group-id)))
  (sync-helpers/broadcast-group-change group-id [:braid.client/new-user (user/user-by-id user-id)]))

(defn register-user!
  [email group-id]
  (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                    :email email}))]
    (user-join-group! (user :id) group-id)
    (user :id)))

