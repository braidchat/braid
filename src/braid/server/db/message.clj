(ns braid.server.db.message
  (:require
    [datomic.api :as d]
    [braid.server.db :as db]
    [braid.server.db.common :refer [message-pull-pattern db->message]]
    [braid.server.db.tag :as tag]
    [braid.server.db.thread :as thread]))

;; Queries

;; Transactions

(defn create-message-txn
  [{:keys [thread-id group-id id content user-id created-at
           mentioned-user-ids mentioned-tag-ids]}]

  ; upsert-thread
  (let [msg-id (d/tempid :entities)
        thread (d/tempid :entities)]
    (concat
      [{:db/id thread
        :thread/id thread-id
        :thread/group [:group/id group-id]}]
      [; create message
       ^{:braid.server.db/return
         (fn [{:keys [db-after tempids]}]
           (->> (d/resolve-tempid db-after tempids msg-id)
                (d/pull db-after message-pull-pattern)
                db->message))}
       {:db/id msg-id
        :message/id id
        :message/content content
        :message/user [:user/id user-id]
        :message/thread thread
        :message/created-at created-at}
       ; user who created message: show thread, subscribe to thread
       [:db/add [:user/id user-id] :user/open-thread thread]
       [:db/add [:user/id user-id] :user/subscribed-thread thread]]
      ; for users subscribed to mentioned tags, open and subscribe them to
      ; the thread
      (mapcat
        (fn [tag-id]
          (into
            [[:db/add thread :thread/tag [:tag/id tag-id]]]
            (mapcat (fn [user-id]
                      [[:db/add [:user/id user-id] :user/subscribed-thread thread]
                       [:db/add [:user/id user-id] :user/open-thread thread]])
                    (tag/users-subscribed-to-tag tag-id))))
        mentioned-tag-ids)
      ; subscribe and open thread for users mentioned
      (mapcat
        (fn [user-id]
          [[:db/add thread :thread/mentioned [:user/id user-id]]
           [:db/add [:user/id user-id] :user/subscribed-thread thread]
           [:db/add [:user/id user-id] :user/open-thread thread]])
        mentioned-user-ids)
      ; open thread for users already subscribed to thread
      (map
        (fn [user-id]
          [:db/add [:user/id user-id] :user/open-thread thread])
        (thread/users-subscribed-to-thread thread-id)))))
