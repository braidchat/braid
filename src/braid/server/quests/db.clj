(ns braid.server.quests.db
  (:require [datomic.api :as d]
            [braid.server.db.common :refer [quest-record-pull-pattern]]))

; getters

(defn get-active-quests-for-user-id [conn user-id]
  (->> (d/q '[:find (pull ?qr pull-pattern)
              :in $ ?user-id pull-pattern
              :where
              [?qr :quest-record/user ?u]
              [?u :user/id ?user-id]
              [?qr :quest-record/state :active]]
            (d/db conn)
            user-id
            quest-record-pull-pattern)
       (map first)))

; setters

(defn upsert-quest-record! [conn user-id quest-record]
  (let [db-id (if (d/entity (d/db conn) [:quest-record/id (quest-record :id)])
                [:quest-record/id (quest-record :quest-record/id)]
                (d/tempid :entities))]
    @(d/transact conn [(assoc quest-record :db/id db-id)])))

(defn activate-first-quests! [conn user-id]
  (let [txs (->> [:quest/quest-complete :quest/conversation-new :quest/conversation-reply]
                 (map (fn [quest-id]
                        {:db/id (d/tempid :entities)
                         :quest-record/id (d/squuid)
                         :quest-record/user [:user/id user-id]
                         :quest-record/quest-id quest-id
                         :quest-record/progress 0
                         :quest-record/state :active})))]
    @(d/transact conn txs)))

