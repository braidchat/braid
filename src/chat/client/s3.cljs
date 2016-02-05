(ns chat.client.s3
  (:require [cljs-uuid-utils.core :as uuid]
            [clojure.string :refer [split]]
            [cljs-utils.core :refer [edn-xhr]]
            [goog.events :as events]
            [chat.client.views.helpers :refer [ends-with?]])
  (:import [goog.net XhrIo EventType]))

(defn- ajax-xhr
  "Send an xhr request"
  [{:keys [method url data on-complete on-error on-progress]}]
  (let [xhr (XhrIo.)]
    (when on-progress
      (events/listen xhr EventType.PROGRESS ; xhrio does not support progress yet
        (fn [e]  (on-progress e))))
    (when on-complete
      (events/listen xhr EventType.SUCCESS
        (fn [e] (on-complete (.getResponseText xhr)))))
    (when on-error
      (events/listen xhr EventType.ERROR
        (fn [e] (on-error {:error (.getResponseText xhr)}))))
    (.send xhr url (.toUpperCase (name method)) data)))

(defn upload [file on-complete]
  (edn-xhr
    {:method :get
     :url (let [path (.. js/window -location -pathname)]
            (str path
                 (when-not (ends-with? path "/") "/")
              "s3-policy"))
     :on-error (fn [err]
                 (.error js/console "Error getting s3 authorization: "
                         (pr-str (:error err))))
     :on-complete
     (fn [{:keys [bucket auth]}]
       (let [file-name (str (uuid/make-random-squuid) "." (last (split (.-type file) #"/")))
             file-url (str "https://s3.amazonaws.com/" bucket "/uploads/" file-name)]
         (ajax-xhr {:method "POST"
                    :url (str "https://s3.amazonaws.com/" bucket)
                    :data (doto (js/FormData.)
                            (.append "key" "uploads/${filename}")
                            (.append "AWSAccessKeyId" (:key auth))
                            (.append "acl" "public-read")
                            (.append "policy" (:policy auth))
                            (.append "signature" (:signature auth))
                            (.append "Content-Type" (.-type file))
                            (.append "file" file file-name))
                    :on-complete (fn [e] (on-complete file-url))
                    :on-error (fn [e] (.error js/console "Error uploading: " (pr-str (:error e))))})))}))
