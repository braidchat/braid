(ns braid.chat.commands
  (:require
    [braid.core.common.schema :as schema]
    [braid.core.server.db :as db]
    [braid.chat.predicates :as p]
    [braid.chat.db.tag :as db.tag]
    [braid.chat.db.group :as db.group]
    [braid.chat.db.thread :as db.thread]
    [braid.chat.db.message :as db.message]
    [braid.core.server.sync-helpers :as sync-helpers]
    [braid.base.server.cqrs-fx :as fx]
    [braid.core.common.util :as util]
    [braid.chat.socket-message-handlers :refer [new-message-callbacks]]))

(def commands
  [{:id :braid.chat/create-group!
    :params {:user-id uuid?
             :group-id uuid?
             :name schema/GroupName
             :slug schema/GroupSlug
             :public? boolean?}
    :conditions
    (fn [{:keys [user-id group-id name slug public?]}]
      [[#(p/user-exists? (db/db) user-id)
        :forbidden "User does not exist"]
       [#(not (p/group-exists? (db/db) group-id))
        :forbidden "Group already exists"]
       [#(not (p/group-with-slug-exists? (db/db) slug))
        :forbidden "Group with this URL already exists"]])
    :effect
    (fn [{:keys [user-id group-id name slug public?]}]
      (fx/run-txns! (db.group/create-group-txn {:id group-id
                                                :slug slug
                                                :name name}))
      (fx/run-txns! (db.group/group-set-txn group-id :public? public?))
      (fx/run-txns! (db.group/user-add-to-group-txn user-id group-id))
      (fx/run-txns! (db.group/user-subscribe-to-group-tags-txn user-id group-id))
      (fx/run-txns! (db.group/user-make-group-admin-txn user-id group-id)))}

   {:id :braid.chat/create-thread!
    :params {:thread-id uuid?
             :group-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id thread-id group-id]}]
      [[#(p/user-exists? (db/db) user-id)
        :not-found "User does not exist"]
       [#(p/group-exists? (db/db) group-id)
        :not-found "Group does not exist"]
       [#(not (p/thread-exists? (db/db) thread-id))
        :forbidden "Thread already exists"]
       [#(p/user-in-group? (db/db) user-id group-id)
        :forbidden "User does not belong to group"]])
    :effect
    (fn [{:keys [user-id thread-id group-id]}]
      (fx/run-txns! (db.thread/create-thread-txn
                      {:user-id user-id
                       :thread-id thread-id
                       :group-id group-id})))}

   {:id :braid.chat/create-tag!
    :params {:user-id uuid?
             :group-id uuid?
             :id uuid?
             :name string?}
    :conditions
    (fn [{:keys [id user-id group-id name]}]
      [;; TODO could be part of :params validation
       [#(util/valid-tag-name? name)
        :forbidden "Tag name invalid"]
       [#(p/user-exists? (db/db) user-id)
        :not-found "User does not exist"]
       [#(p/group-exists? (db/db) group-id)
        :not-found "Group does not exist"]
       [#(not (p/tag-exists? (db/db) id))
        :forbidden "Tag with this id already exists"]
       [#(not (p/tag-in-group-with-name-exists? (db/db) group-id name))
        :forbidden "Tag with this name already exists"]
       [#(p/user-in-group? (db/db) user-id group-id)
        :forbidden "User does not belong to group"]])
    :effect
    (fn [{:keys [id name group-id]}]
      (let [[new-tag] (fx/run-txns!
                        (db.tag/create-tag-txn
                          {:id id
                           :name name
                           :group-id group-id}))]
        ;; subscribe all users in group
        (fx/run-txns!
          (mapcat
            (fn [user] (db.tag/user-subscribe-to-tag-txn (user :id) id))
            (db.group/group-users (:group-id new-tag))))
        (fx/group-broadcast! group-id [:braid.client/create-tag new-tag])))}

   {:id :braid.chat/create-message!
    :params {:user-id uuid?
             :thread-id uuid?
             :message-id uuid?
             :content string?
             ;; TODO should probably regenerate these from content
             ;; rather than trusting the client side
             :mentioned-tag-ids [uuid?]
             :mentioned-user-ids [uuid?]}
    :conditions
    (fn [{:keys [user-id thread-id message-id content
                 mentioned-tag-ids mentioned-user-ids]}]
      (concat
        [[#(p/user-exists? (db/db) user-id)
          :not-found "User does not exist"]
         [#(p/thread-exists? (db/db) thread-id)
          :not-found "Thread does not exist"]
         [#(not (p/message-exists? (db/db) message-id))
          :forbidden "Message already exists"]
         [#(< (count content) 5000)
          :forbidden "Content too long. Max length is 5000 chars."]
         [#(p/user-can-access-thread? (db/db) user-id thread-id)
          :forbidden "User cannot access this thread."]]
        (for [tag-id mentioned-tag-ids]
          [#(p/tag-exists? (db/db) tag-id)
           :forbidden (str "Tag " tag-id " does not exist")])
        (for [user-id mentioned-user-ids]
          [#(p/user-exists? (db/db) user-id)
           :forbidden (str "User " user-id " does not exist")])
        (for [tag-id mentioned-tag-ids]
          [#(p/thread-tag-same-group? (db/db) thread-id tag-id)
           :forbidden (str "Tag " tag-id " not in same group as thread")])
        (for [user-id mentioned-user-ids]
          [#(p/thread-user-same-group? (db/db) thread-id user-id)
           :forbidden (str "User " user-id " not in same group as thread")])))
    :effect
    (fn [{:keys [user-id thread-id message-id content
                 mentioned-tag-ids mentioned-user-ids]}]
      (let [message {:id message-id
                     :user-id user-id
                     :thread-id thread-id
                     :created-at (java.util.Date.)
                     :content content
                     :mentioned-tag-ids mentioned-tag-ids
                     :mentioned-user-ids mentioned-user-ids}]
        (fx/run-txns! (db.message/create-message-txn message))

        (doseq [callback @new-message-callbacks]
          (callback message))

        (sync-helpers/broadcast-thread (message :thread-id) [])

        (sync-helpers/notify-users message)))}])


