(ns braid.server.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [braid.common.util :refer [extract]]
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

(defn db [] (d/db conn))

(defn uuid
  []
  (d/squuid))

(defn run-txns!
  "Execute given transactions. Transactions may be annotated with metadata.
  If the metadata map contains a function under the key `:return`, that
  function will be called with the transaction result and the return value
  added to the seq of return values
  If the metadata map contains a function under the key
  `:braid.server.db/check`, that function will be called with the transaction
  result and if it fails an assert, the transaction will be aborted & an error
  bubbled up via ex-info, with the assertion failure message under the key
  `:braid.server.db/error`."
  [txns]
  (let [[returns checks] ((juxt (partial extract :return)
                                (partial extract ::check))
                          (map meta txns))]
    (when (seq checks)
      (let [test-db (d/with (db) txns)]
        (try
          (doseq [check checks]
            (check test-db))
          (catch AssertionError e
            (throw (ex-info "Transaction Failed"
                            {::error (.getMessage e)}))))))
    (let [tx-result @(d/transact conn txns)]
      (map #(% tx-result) returns))))
