(ns braid.core.server.conf
  (:require
   [braid.core.module-helpers :refer [defhook]]
   [environ.core :refer [env]]
   [mount.core :as mount :refer [defstate]]))

(defhook
  :writer register-config-var!
  :reader config-vars)

(defstate config
  :start
  (merge {:db-url "datomic:mem://braid"
          :api-domain (str "localhost:" (+ 2 (:port (mount/args))))
          :site-url (str "http://localhost:" (:port (mount/args)))
          :hmac-secret "secret"}
         (select-keys env
                      (into [:api-domain
                             :asana-client-id
                             :asana-client-secret
                             :aws-access-key
                             :aws-domain
                             :aws-secret-key
                             :db-url
                             :elasticsearch-url
                             :embedly-key
                             :environment
                             :oauth-provider-list
                             :hmac-secret
                             :mailgun-domain
                             :mailgun-password
                             :s3-upload-key
                             :s3-upload-secret
                             :site-url]
                            @config-vars))))
