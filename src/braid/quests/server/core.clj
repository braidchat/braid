(ns braid.quests.server.core
  (:require
    [braid.core.api :as api]
    [braid.quests.server.db :as db]
    [datomic.db]))

(defn init! []
  (api/dispatch [:braid.core/register-db-schema!
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
                   :db.install/_attribute :db.part/db}]])

  (api/dispatch [:braid.core/register-initial-user-data!
                 (fn [user-id]
                   {:quest-records (db/get-active-quests-for-user-id user-id)})]))

