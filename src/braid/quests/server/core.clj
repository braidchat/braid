(ns braid.quests.server.core
  (:require
    [braid.core.api :as api]
    [braid.quests.server.db :as db]
    [braid.server.sync-handler] ; for mount
    [braid.server.sync :refer [register-initial-user-data!]]
    [braid.server.schema :refer [register-db-schema!]]
    [braid.server.sync-handler :refer [register-server-message-handler!]]
    [datomic.db] ; for reader macro
    [mount.core :refer [defstate]]))

(defn init! []
  (register-db-schema!
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

  (register-initial-user-data!
    (fn [user-id]
      {:quest-records (db/get-active-quests-for-user-id user-id)}))

  (register-server-message-handler!
    :braid.server.quests/upsert-quest-record
    (fn [{:keys [?data user-id]}]
      {:chsk-send! [user-id [:braid.quests/upsert-quest-record ?data]]
       :db-run-txns! (db/upsert-quest-record-txn user-id ?data)})))

(defstate quests
  :start (init!))
