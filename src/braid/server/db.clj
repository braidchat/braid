(ns braid.server.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [braid.server.conf :refer [config]]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [braid.server.schema :refer [schema]]
            [braid.server.db user message group invitation thread tag bot
             upload]))

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

(defmacro reexport-db-ns
  "Take a namespace for database functions and re-export it in this namespace.
  This assumes that all the public functions in the namespace have the database
  connection as the first argument, which will be automatically set to `conn`
  in the wrapped functions"
  [db-ns]
  (let [pub-fns (ns-publics db-ns)]
    `(do
       ~@(doall
           (for [[fn-name fn-var] pub-fns]
             (let [{args :arglists :as info} (meta fn-var)]
               `(defn ~fn-name
                  ; preserve the same arglist as the original function, so one
                  ; can see, for example :keys in the arglist
                  {:arglists '~(map (comp vec rest) args)
                   :doc ~(info :doc)}
                  ~@(doall
                      (for [as args]
                        (let [gargs (repeatedly (dec (count as)) gensym)]
                          `(~(vec gargs) (~fn-var conn ~@gargs))))))))))))

(reexport-db-ns braid.server.db.user)

(reexport-db-ns braid.server.db.message)

(reexport-db-ns braid.server.db.group)

(reexport-db-ns braid.server.db.invitation)

(reexport-db-ns braid.server.db.thread)

(reexport-db-ns braid.server.db.tag)

(reexport-db-ns braid.server.db.bot)

(reexport-db-ns braid.server.db.upload)
