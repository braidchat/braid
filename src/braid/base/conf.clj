(ns braid.base.conf
  (:require
   [braid.core.hooks :as hooks]
   [environ.core :refer [env]]
   [mount.core :as mount :refer [defstate]]))

(defonce config-vars
  (hooks/register! (atom []) [keyword?]))

(defstate config
  :start
  (merge ;; temp defaults
         ;; TODO don't special case these here
         ;; ports should come from config
         {:db-url "datomic:mem://braid"
          :api-domain (str "localhost:" (+ 2 (:port (mount/args))))
          :site-url (str "http://localhost:" (:port (mount/args)))
          :hmac-secret "secret"}
         (select-keys env @config-vars)))
