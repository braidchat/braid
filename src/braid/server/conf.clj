(ns braid.server.conf
  (:require [environ.core :refer [env]]
            [mount.core :as mount :refer [defstate]]))

(defstate config
  :start
  (merge {:db-url "datomic:free://localhost:4334/braid"
          :api-domain (str "localhost:" (+ 2 (:port (mount/args))))}
         (select-keys env
                      [:mailgun-domain
                       :mailgun-password
                       :site-url
                       :hmac-secret
                       :aws-domain
                       :aws-access-key
                       :aws-secret-key
                       :db-url
                       :environment
                       :s3-upload-key
                       :s3-upload-secret
                       :asana-client-id
                       :asana-client-secret
                       :embedly-key
                       :api-domain])))
