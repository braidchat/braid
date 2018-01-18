(ns braid.test.fixtures.db
  (:require
    [mount.core :as mount]
    [braid.server.db :as db]
    [braid.server.conf :as conf]
    [braid.server.schema]
    [braid.core.modules]
    [braid.state.core]))

(defn drop-db [t]
  (-> (mount/only #{#'conf/config  #'braid.state.core/state #'db/conn})
      (mount/swap {#'conf/config
                   {:db-url "datomic:mem://chat-test"}})
      (mount/start))
  (t)
  (datomic.api/delete-database (conf/config :db-url))
  (mount/stop))
