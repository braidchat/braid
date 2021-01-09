(ns braid.test.fixtures.db
  (:require
   [braid.core.modules :as modules]
   [braid.base.conf :as conf]
   [braid.core.server.db :as db]
   [datomic.api]
   [mount.core :as mount]))

(defn drop-db [t]
  (modules/init! modules/default)
  (-> (mount/only #{#'conf/config #'db/conn})
      (mount/with-args {:port 0
                        :db-url "datomic:mem://chat-test"})
      (mount/start))
  (t)
  (datomic.api/delete-database (conf/config :db-url))
  (mount/stop))
