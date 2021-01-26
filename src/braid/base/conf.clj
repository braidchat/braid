(ns braid.base.conf
  (:require
   [braid.base.conf-extra :refer [ports-config]]
   [braid.core.hooks :as hooks]
   [mount.core :as mount :refer [defstate]]))

(defonce config-vars
  (hooks/register! (atom []) [keyword?]))

(defstate config
  :start
  (merge ;; temp defaults
    ;; TODO don't special case these here
    ;; ports should come from config
    {:db-url "datomic:mem://braid"
     :site-url (str "http://localhost:" (:port (mount/args)))
     :hmac-secret "secret"
     :app-title "Braid"}
    @ports-config ; overrides site url when port is automatically found
    (select-keys (mount/args) @config-vars)))
