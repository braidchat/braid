(ns braid.server.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [braid.server.conf :refer [config]]
            [braid.server.schema :refer [schema]]))

(defn init!
  "set up schema"
  [db-url]
  (when (d/create-database db-url)
    @(d/transact (d/connect db-url)
                 (concat
                   ; partition for our data
                   [{:db/ident :entities
                     :db/id #db/id [:db.part/db]
                     :db.install/_partition :db.part/db}]
                   schema))))

(defn connect [{:keys [db-url]}]
  (init! db-url)
  (d/connect db-url))

(defstate conn
  :start (connect config))

(defn uuid
  []
  (d/squuid))

(defn run-txns!
  "Execute given transactions"
  ; TODO: If all txns go through this, can now use this place to check
  ; validity, maybe use with-db to speculatively execute & validate
  [txns]
  (d/transact conn txns))
