(ns braid.quests.server.core
  (:require
    [datomic.db] ; for reader macro
    [braid.quests.server.db :as db]))

(def db-schema
  [{:db/ident :quest-record/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :quest-record/quest-id
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :quest-record/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :quest-record/state
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :quest-record/progress
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

(defn initial-user-data-fn
  [user-id]
  {:quest-records (db/get-active-quests-for-user-id user-id)})

(def server-message-handlers
  {:braid.server.quests/upsert-quest-record
   (fn [{:keys [?data user-id]}]
     {:chsk-send! [user-id [:braid.quests/upsert-quest-record ?data]]
      :db-run-txns! (db/upsert-quest-record-txn user-id ?data)})})
