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

; for debugging only:
(defn get-quests-for-user-id [conn user-id]
  (->> (d/q '[:find (pull ?qr pull-pattern)
              :in $ ?user-id pull-pattern
              :where
              [?qr :quest-record/user ?u]
              [?u :user/id ?user-id]]
            (d/db conn)
            user-id
            quest-record-pull-pattern)
       (map first)))

; setters

(defn- update-quest-record! [conn user-id quest-record-id f]
  (when-let [quest-record (-> (d/q '[:find (pull ?qr pull-pattern)
                                     :in $ ?quest-record-id pull-pattern
                                     :where
                                     [?qr :quest-record/id ?quest-record-id]]
                                   (d/db conn) quest-record-id quest-record-pull-pattern)
                              ffirst)]
    @(d/transact conn [(assoc (f quest-record)
                         :db/id [:quest-record/id (quest-record :quest-record/id)])])))

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

(defn store-quest-record! [conn user-id quest-record]
  @(d/transact conn [(assoc quest-record
                       :db/id (d/tempid :entities)
                       :quest-record/user [:user/id user-id])]))

(defn increment-quest! [conn user-id quest-record-id]
  (update-quest-record! conn user-id quest-record-id (fn [quest-record]
                                                       (update quest-record :quest-record/progress inc))))

(defn skip-quest! [conn user-id quest-record-id]
  (update-quest-record! conn user-id quest-record-id (fn [quest-record]
                                                       (assoc quest-record :quest-record/state :skipped))))

(defn complete-quest! [conn user-id quest-record-id]
  (update-quest-record! conn user-id quest-record-id (fn [quest-record]
                                                       (assoc quest-record :quest-record/state :complete))))

