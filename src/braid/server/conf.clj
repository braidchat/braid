(ns braid.server.conf
  (:require [environ.core :refer [env]]
            [mount.core :as mount :refer [defstate]]))

(defstate config
  :start
  (merge {:db-url "datomic:free://localhost:4334/braid"
          :api-domain (str "localhost:" (+ 2 (:port (mount/args))))
          :hmac-secret "secret"}
         (select-keys env
                      [:api-domain
                       :asana-client-id
                       :asana-client-secret
                       :aws-access-key
                       :aws-domain
                       :aws-secret-key
                       :db-url
                       :embedly-key
                       :environment
                       :github-client-id
                       :github-client-secret
                       :hmac-secret
                       :mailgun-domain
                       :mailgun-password
                       :s3-upload-key
                       :s3-upload-secret
                       :site-url])))
