(ns braid.page-inbox.commands
  (:require
    [braid.core.server.db :as db]
    [braid.base.server.socket :as socket]
    [braid.chat.cq-helpers :as h]
    [braid.chat.db.thread :as db.thread]))

(def commands
  [{:id :braid.inbox/hide-thread!
    :params {:thread-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id thread-id]}]
      [[#(h/user-exists? (db/db) user-id)
        :not-found "User does not exist"]
       [#(h/thread-exists? (db/db) thread-id)
        :not-found "Thread does not exist"]
       [#(h/user-has-thread-open? (db/db) user-id thread-id)
        :forbidden "User does not have thread open"]])
    :effect
    (fn [{:keys [user-id thread-id]}]
      (db/run-txns!
        (db.thread/user-hide-thread-txn user-id thread-id))
      (socket/chsk-send! user-id [:braid.client/hide-thread thread-id]))}

   {:id :braid.inbox/show-thread!
    :params {:thread-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id thread-id]}]
      [[#(h/user-exists? (db/db) user-id)
        :not-found "User does not exist"]
       [#(h/thread-exists? (db/db) thread-id)
        :not-found "Thread does not exist"]
       [#(not (h/user-has-thread-open? (db/db) user-id thread-id))
        :forbidden "User already has thread open"]
       [#(h/user-can-access-thread? (db/db) user-id thread-id)
        :forbidden "User cannot access this thread"]])
    :effect
    (fn [{:keys [user-id thread-id]}]
      (db/run-txns! (db.thread/user-show-thread-txn user-id thread-id))
      (socket/chsk-send! user-id [:braid.client/show-thread
                                  (db.thread/thread-by-id thread-id)]))}])
