(ns braid.chat.commands
  (:require
    [braid.core.server.db :as db]
    [braid.chat.cq-helpers :as h]
    [braid.chat.db.thread :as db.thread]))

(def commands
  [{:id :braid.chat/create-thread!
    :params {:thread-id uuid?
             :group-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id thread-id group-id]}]
      [[#(h/user-exists? (db/db) user-id)
        :not-found "User does not exist"]
       [#(h/group-exists? (db/db) group-id)
        :not-found "Group does not exist"]
       [#(not (h/thread-exists? (db/db) thread-id))
        :forbidden "Thread already exists"]
       [#(h/user-in-group? (db/db) user-id group-id)
        :forbidden "User does not belong to group"]])
    :effect
    (fn [{:keys [user-id thread-id group-id]}]
      (db/run-txns! (db.thread/create-thread-txn
                      {:user-id user-id
                       :thread-id thread-id
                       :group-id group-id})))}])

