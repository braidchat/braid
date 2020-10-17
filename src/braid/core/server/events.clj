(ns braid.core.server.events
  (:require
    [braid.core.server.db :as db]
    [braid.chat.db.group :as group]
    [braid.chat.db.thread :as thread]
    [braid.chat.db.user :as user]
    [braid.core.server.sync-helpers :as sync-helpers]))

(defn user-join-group!
  [user-id group-id]
  (db/run-txns! (group/user-join-group-txn user-id group-id))
  (sync-helpers/broadcast-group-change
    group-id
    [:braid.client/new-user [(-> (user/user-by-id user-id)
                                 (assoc :joined-at (java.util.Date.)))
                             group-id]]))

(defn register-user!
  [email group-id]
  (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                    :email email}))]
    (user-join-group! (user :id) group-id)
    (user :id)))
