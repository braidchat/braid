(ns braid.core.server.uploads
  (:require
   [braid.core.server.conf :refer [config]]
   [braid.uploads.s3 :refer [make-request]]
   [clj-time.core :as t]
   [clj-time.format :as f]
   [org.httpkit.client :as http]))

(defn upload-url-path
  [url]
  (some->
    (re-pattern (str "^https://s3.amazonaws.com/"
                     (config :aws-domain)
                     "(/.*)$"))
    (re-matches url)
    second))

(defn delete-upload
  [upload-path]
  @(http/request (make-request {:method :delete
                                :body ""
                                :path upload-path})))
