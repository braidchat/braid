(ns braid.quests.server.core
  (:require
    [datomic.db] ; for reader macro
    [braid.core.hooks :as hooks]
    [braid.quests.server.db :as db]
    [braid.server.sync :refer [initial-user-data]]
    [braid.server.schema :refer [schema]]
    [braid.server.sync-handler :refer [message-handler]]))

(hooks/def-data-hook-extension schema quests-schema
  []
  [{:db/ident :quest-record/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/id #db/id [:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :quest-record/quest-id
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/id #db/id [:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :quest-record/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/id #db/id [:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :quest-record/state
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/id #db/id [:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :quest-record/progress
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/id #db/id [:db.part/db]
    :db.install/_attribute :db.part/db}])

(hooks/def-data-hook-extension initial-user-data quests-data
  [user-id]
  {:quest-records (db/get-active-quests-for-user-id user-id)})

(hooks/def-data-hook-extension message-handler quest-message-handler
  []
  {:braid.server.quests/upsert-quest-record
   (fn [{:keys [?data user-id]}]
     {:chsk-send! [user-id [:braid.quests/upsert-quest-record ?data]]
      :db-run-txns! (db/upsert-quest-record-txn user-id ?data)})})
