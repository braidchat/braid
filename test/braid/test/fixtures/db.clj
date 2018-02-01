(ns braid.test.fixtures.db
  (:require
    [mount.core :as mount]
    [braid.server.db :as db]
    [braid.server.conf :as conf]
    [braid.core.modules :as modules]))

(defn drop-db [t]
  (modules/init!)
  (-> (mount/only #{#'conf/config #'db/conn})
      (mount/swap {#'conf/config
                   {:db-url "datomic:mem://chat-test"}})
      (mount/start))
  (t)
  (datomic.api/delete-database (conf/config :db-url))
  (mount/stop))
