(ns braid.chat.db.message
  (:require
    [braid.core.server.db :as db]
    [braid.chat.db.common :refer [message-pull-pattern db->message]]
    [braid.chat.db.tag :as tag]
    [braid.chat.db.thread :as thread]
    [datomic.api :as d]))

;; Queries

(defn message-group
  [message-id]
  (d/q '[:find ?group-id .
         :in $ ?msg-id
         :where
         [?m :message/id ?msg-id]
         [?m :message/thread ?t]
         [?t :thread/group ?g]
         [?g :group/id ?group-id]]
       (db/db) message-id))

(defn message-thread
  [message-id]
  (d/q '[:find ?thread-id .
         :in $ ?msg-id
         :where
         [?m :message/id ?msg-id]
         [?m :message/thread ?t]
         [?t :thread/id ?thread-id]]
       (db/db) message-id))

(defn message-author
  [message-id]
  (d/q '[:find ?user-id .
         :in $ ?msg-id
         :where
         [?m :message/id ?msg-id]
         [?m :message/user ?u]
         [?u :user/id ?user-id]]
       (db/db) message-id))

;; Transactions

(defn create-message-txn
  [{:keys [thread-id id content user-id created-at
           mentioned-user-ids mentioned-tag-ids]}]
  (let [msg-id (d/tempid :entities)]
    (concat
      [; create message
       ^{:braid.core.server.db/return
         (fn [{:keys [db-after tempids]}]
           (->> (d/resolve-tempid db-after tempids msg-id)
                (d/pull db-after message-pull-pattern)
                db->message))}
       {:db/id msg-id
        :message/id id
        :message/content content
        :message/user [:user/id user-id]
        :message/thread [:thread/id thread-id]
        :message/created-at created-at}
       ; user who created message: show thread, subscribe to thread
       [:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]
       [:db/add [:user/id user-id] :user/subscribed-thread [:thread/id thread-id]]]
      ; for users subscribed to mentioned tags, open and subscribe them to
      ; the thread
      (mapcat
        (fn [tag-id]
          (into
            [[:db/add [:thread/id thread-id] :thread/tag [:tag/id tag-id]]]
            (mapcat (fn [user-id]
                      [[:db/add [:user/id user-id] :user/subscribed-thread [:thread/id thread-id]]
                       [:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]])
                    (tag/users-subscribed-to-tag tag-id))))
        mentioned-tag-ids)
      ; subscribe and open thread for users mentioned
      (mapcat
        (fn [user-id]
          [[:db/add [:thread/id thread-id] :thread/mentioned [:user/id user-id]]
           [:db/add [:user/id user-id] :user/subscribed-thread [:thread/id thread-id]]
           [:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]])
        mentioned-user-ids)
      ; open thread for users already subscribed to thread
      (map
        (fn [user-id]
          [:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]])
        (thread/users-subscribed-to-thread thread-id)))))


(defn retract-message-txn
  [message-id]
  [[:db.fn/retractEntity [:message/id message-id]]])
