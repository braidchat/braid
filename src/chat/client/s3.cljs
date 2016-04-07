(ns chat.client.s3
  (:require [cljs-uuid-utils.core :as uuid]
            [clojure.string :refer [split]]
            [goog.events :as events]
            [chat.client.xhr :refer [edn-xhr ajax-xhr]]
            [chat.client.views.helpers :refer [ends-with?]])
  (:import [goog.net XhrIo EventType]))

(defn upload [file on-complete]
  (edn-xhr
    {:method :get
     :uri "/s3-policy"
     :on-error (fn [err]
                 (.error js/console "Error getting s3 authorization: "
                         (pr-str (:error err))))
     :on-complete
     (fn [{:keys [bucket auth]}]
       (let [file-name (str (uuid/make-random-squuid) "." (last (split (.-type file) #"/")))
             file-url (str "https://s3.amazonaws.com/" bucket "/uploads/" file-name)]
         (ajax-xhr {:method :post
                    :uri (str "https://s3.amazonaws.com/" bucket)
                    :body (doto (js/FormData.)
                            (.append "key" "uploads/${filename}")
                            (.append "AWSAccessKeyId" (:key auth))
                            (.append "acl" "public-read")
                            (.append "policy" (:policy auth))
                            (.append "signature" (:signature auth))
                            (.append "Content-Type" (.-type file))
                            (.append "file" file file-name))
                    :on-complete (fn [e] (on-complete file-url))
                    :on-error (fn [e] (.error js/console "Error uploading: " (pr-str (:error e))))})))}))
