(ns braid.server.conf
  (:require [environ.core :refer [env]]
            [mount.core :refer [defstate]]))

(defstate config
  :start (select-keys env
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
                       ]))
