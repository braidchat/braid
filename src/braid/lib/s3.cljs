(ns braid.lib.s3
  (:require
   [braid.lib.xhr :refer [edn-xhr ajax-xhr]]
   [taoensso.timbre :as timbre :refer-macros [errorf]]))

(defn s3-path [bucket region & paths]
  (str "https://" bucket ".s3." region ".amazonaws.com/" (apply str paths)))

(defn upload
  [{:keys [file prefix on-complete]}]
  (edn-xhr
    {:method :get
     :uri "/s3-policy"
     :on-error (fn [err]
                 (errorf "Error getting s3 authorization: %s" err))
     :on-complete
     (fn [{:keys [bucket region auth]}]
       (let [file-name (.-name file)
             file-key (str prefix (js/encodeURIComponent file-name))
             file-url (s3-path bucket region file-key)]
         (ajax-xhr {:method :post
                    :uri (s3-path bucket region)
                    :body (doto (js/FormData.)
                            (.append "key" (str prefix "${filename}"))
                            (.append "acl" "private")
                            (.append "policy" (:policy auth))
                            (.append "x-amz-algorithm" "AWS4-HMAC-SHA256")
                            (.append "x-amz-credential" (:credential auth))
                            (.append "x-amz-signature" (:signature auth))
                            (.append "x-amz-date" (:date auth))
                            (cond->
                                (:security-token auth)
                              (.append "x-amz-security-token"
                                       (:security-token auth)))
                            (.append "Content-Type" (.-type file))
                            (.append "file" file file-name))
                    :on-complete (fn [_]
                                   (on-complete {:key file-key
                                                 :url file-url}))
                    :on-error (fn [e] (errorf "Error uploading: %s" (:error e)))})))}))
