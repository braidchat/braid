(ns chat.server.migrate
  (:require [chat.server.db :as db]
            [datomic.api :as d]))

(defn migrate-2015-07-29
  "Schema changes for groups"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :tag/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}

       ; groups
       {:db/ident :group/id
        :db/valueType :db.type/uuid
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :group/name
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :group/user
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))
