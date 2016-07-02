(ns braid.client.s3
  (:require [cljs-uuid-utils.core :as uuid]
            [clojure.string :refer [split]]
            [goog.events :as events]
            [taoensso.timbre :as timbre :refer-macros [errorf]]
            [braid.client.xhr :refer [edn-xhr ajax-xhr]])
  (:import [goog.net XhrIo EventType]))

(defn upload [file on-complete]
  (edn-xhr
    {:method :get
     :uri "/s3-policy"
     :on-error (fn [err]
                 (errorf "Error getting s3 authorization: %s" (:error err)))
     :on-complete
     (fn [{:keys [bucket auth]}]
       (let [file-name (.-name file)
             file-dir (uuid/make-random-squuid)
             file-url (str "https://s3.amazonaws.com/" bucket "/uploads/" file-dir
                           "/" (js/encodeURIComponent file-name))]
         (ajax-xhr {:method :post
                    :uri (str "https://s3.amazonaws.com/" bucket)
                    :body (doto (js/FormData.)
                            (.append "key" (str "uploads/" file-dir "/${filename}"))
                            (.append "AWSAccessKeyId" (:key auth))
                            (.append "acl" "public-read")
                            (.append "policy" (:policy auth))
                            (.append "signature" (:signature auth))
                            (.append "Content-Type" (.-type file))
                            (.append "file" file file-name))
                    :on-complete (fn [e] (on-complete file-url))
                    :on-error (fn [e] (errorf "Error uploading: %s" (:error e)))})))}))
