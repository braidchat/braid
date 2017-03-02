(ns braid.server.quests.db
  (:require
    [datomic.api :as d]
    [braid.server.db :as db]
    [braid.server.db.common :refer [quest-record-pull-pattern]]))

; Queries

(defn get-active-quests-for-user-id
  [user-id]
  (->> (d/q '[:find (pull ?qr pull-pattern)
              :in $ ?user-id pull-pattern
              :where
              [?qr :quest-record/user ?u]
              [?u :user/id ?user-id]
              [?qr :quest-record/state :active]]
            (db/db)
            user-id
            quest-record-pull-pattern)
       (map first)))

; Transactions

(defn upsert-quest-record-txn
  [user-id quest-record]
  (let [db-id (if (d/entity (db/db) [:quest-record/id (quest-record :id)])
                [:quest-record/id (quest-record :quest-record/id)]
                (d/tempid :entities))]
    [(assoc quest-record :db/id db-id)]))

(defn activate-first-quests-txn
  [user-id]
  (->> [:quest/quest-complete :quest/conversation-new :quest/conversation-reply]
       (map (fn [quest-id]
              {:db/id (d/tempid :entities)
               :quest-record/id (d/squuid)
               :quest-record/user user-id
               :quest-record/quest-id quest-id
               :quest-record/progress 0
               :quest-record/state :active}))))
