(ns braid.server.events
  (:require
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.db.thread :as thread]
    [braid.server.db.user :as user]
    [braid.server.sync-helpers :as sync-helpers]))

(defn user-join-group!
  [user-id group-id]
  (db/run-txns! (group/user-join-group-txn user-id group-id))
  (sync-helpers/broadcast-group-change
    group-id
    [:braid.client/new-user (user/user-by-id user-id)]))

(defn register-user!
  [email group-id]
  (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                    :email email}))]
    (user-join-group! (user :id) group-id)
    (user :id)))
