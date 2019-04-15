(ns braid.core.server.conf
  (:require
   [braid.core.hooks :as hooks]
   [environ.core :refer [env]]
   [mount.core :as mount :refer [defstate]]))

(defonce config-vars
  (hooks/register! (atom []) [keyword?]))

(defstate config
  :start
  (merge {:db-url "datomic:mem://braid"
          :api-domain (str "localhost:" (+ 2 (:port (mount/args))))
          :site-url (str "http://localhost:" (:port (mount/args)))
          :hmac-secret "secret"}
         (select-keys env
                      (into
                        [:api-domain
                         :asana-client-id
                         :asana-client-secret
                         :aws-access-key
                         :aws-domain
                         :aws-region
                         :aws-secret-key
                         :db-url
                         :embedly-key
                         :environment
                         :github-client-id
                         :github-client-secret
                         :hmac-secret
                         :mailgun-domain
                         :mailgun-password
                         :site-url]
                        @config-vars))))
