(ns braid.lib.upload
  (:require
   [clojure.string :as string]))

(def regex (delay
             (re-pattern (str "https?://"
                              (-> (aget js/window "asset_domain")
                                  (string/replace "." "\\."))
                              "/"))))

(defn ->path [url]
  (string/replace
   url
   @regex
   (str "//" (aget js/window "api_domain") "/upload/")))

(defn upload-path? [url]
  (string/includes? url (aget js/window "asset_domain")))
