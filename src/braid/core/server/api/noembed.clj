(ns braid.core.server.api.noembed
  (:require
    [clojure.data.json :as json]
    [org.httpkit.client :as http]
    [taoensso.timbre :as timbre]))

(defn get-oembed
  [url]
  (-> @(http/request {:url "http://noembed.com/embed"
                      :query-params {:url url}})
      :body
      (json/read-str :key-fn keyword)))
