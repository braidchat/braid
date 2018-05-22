(ns braid.core.server.uploads
  (:require
   [aws.sdk.s3 :as s3]
   [braid.core.server.conf :refer [config]]))

(defn upload-url-path
  [url]
  (some->
    (re-pattern (str "^https://s3.amazonaws.com/"
                     (config :aws-domain)
                     "/(.*)$"))
    (re-matches url)
    second))

(defn delete-upload
  [upload-path]
  (let [creds {:access-key (config :aws-access-key)
               :secret-key (config :aws-secret-key)}]
    (s3/delete-object creds (config :aws-domain) upload-path)))
