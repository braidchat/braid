(ns braid.server.db.message
  (:require [datomic.api :as d]
            [braid.server.db.common :refer :all]
            [braid.server.db.tag :as tag]
            [braid.server.db.thread :as thread]))

(defn create-message!
  [conn {:keys [thread-id group-id id content user-id created-at
                mentioned-user-ids mentioned-tag-ids]}]

  ; upsert-thread
  (when-not (d/entity (d/db conn) [:thread/id thread-id])
    @(d/transact conn (concat [{:db/id (d/tempid :entities)
                                :thread/id thread-id
                                :thread/group [:group/id group-id]}])))

  (let [; for users subscribed to mentioned tags, open and subscribe them to
        ; the thread
        txs-for-tag-mentions (mapcat
                               (fn [tag-id]
                                 (into
                                   [[:db/add [:thread/id thread-id]
                                     :thread/tag [:tag/id tag-id]]]
                                   (mapcat (fn [user-id]
                                             [[:db/add [:user/id user-id]
                                               :user/subscribed-thread
                                               [:thread/id thread-id]]
                                              [:db/add [:user/id user-id]
                                               :user/open-thread
                                               [:thread/id thread-id]]])
                                           (tag/users-subscribed-to-tag conn tag-id))))
                               mentioned-tag-ids)
        ; subscribe and open thread for users mentioned
        txs-for-user-mentions (mapcat
                                (fn [user-id]
                                  [[:db/add [:thread/id thread-id]
                                    :thread/mentioned [:user/id user-id]]
                                   [:db/add [:user/id user-id]
                                    :user/subscribed-thread [:thread/id thread-id]]
                                   [:db/add [:user/id user-id]
                                    :user/open-thread [:thread/id thread-id]]])
                                mentioned-user-ids)
        ; open thread for users already subscribed to thread
        txs-for-tag-subscribers (map
                                  (fn [user-id]
                                    [:db/add [:user/id user-id]
                                     :user/open-thread [:thread/id thread-id]])
                                  (thread/users-subscribed-to-thread conn thread-id))
        ; upsert message
        msg-data {:db/id (d/tempid :entities)
                  :message/id id
                  :message/content content
                  :message/user [:user/id user-id]
                  :message/thread [:thread/id thread-id]
                  :message/created-at created-at}
        ; user who created message: show thread, subscribe to thread
        subscribe-data [[:db/add [:user/id user-id]
                         :user/open-thread [:thread/id thread-id]]
                        [:db/add [:user/id user-id]
                         :user/subscribed-thread [:thread/id thread-id]]]
        {:keys [db-after tempids]} @(d/transact conn
                                      (concat [msg-data]
                                              subscribe-data
                                              txs-for-tag-subscribers
                                              txs-for-tag-mentions
                                              txs-for-user-mentions))]
    (->> (d/resolve-tempid db-after tempids (msg-data :db/id))
         (d/pull db-after message-pull-pattern)
         db->message)))

