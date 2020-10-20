(ns braid.test.fixtures.db
  (:require
   [braid.core.modules :as modules]
   [braid.base.conf :as conf]
   [braid.core.server.db :as db]
   [mount.core :as mount]))

(defn drop-db [t]
  (modules/init!)
  (-> (mount/only #{#'conf/config #'db/conn})
      (mount/swap {#'conf/config
                   {:db-url "datomic:mem://chat-test"}})
      (mount/start))
  (t)
  (datomic.api/delete-database (conf/config :db-url))
  (mount/stop))
