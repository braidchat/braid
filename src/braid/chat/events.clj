(ns braid.chat.events
  (:require
    [braid.core.server.db :as db]
    [braid.chat.db.group :as group]
    [braid.chat.db.thread :as thread]
    [braid.chat.db.user :as user]
    [braid.core.server.sync-helpers :as sync-helpers]))

(defn user-join-group!
  [user-id group-id]
  (db/run-txns! (group/user-join-group-txn user-id group-id))
  (db/run-txns! (group/user-open-recent-threads user-id group-id))
  (sync-helpers/broadcast-group-change
    group-id
    [:braid.client/new-user [(-> (user/user-by-id user-id)
                                 (assoc :joined-at (java.util.Date.)))
                             group-id]]))

(defn register-user!
  [{:keys [email password group-id]}]
  (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                    :email email}))]
    (db/run-txns! (user/set-user-password-txn (:id user) password))
    (user-join-group! (user :id) group-id)
    (user :id)))
