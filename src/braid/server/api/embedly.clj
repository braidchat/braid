(ns braid.server.api.embedly
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [braid.server.conf :refer [config]]
            [clojure.data.json :as json]))

(defn get-json
  "Don't call this directly, prefer `extract`"
  [url]
  (->
    @(http/request {:url "https://api.embedly.com/1/extract"
                    :query-params {"key" (config :embedly-key)
                                   "url" url
                                   "maxwidth" 275
                                   "maxheight" 275
                                   "format" "json"
                                   "secure" true}})
    :body
    (json/read-str :key-fn keyword)))

(def get-json-memo
  (memoize get-json))

(defn extract [url]
  (when (config :embedly-key)
    (try
      (get-json-memo url)
      (catch java.io.EOFException ex
        (timbre/warnf "EOF while parsing embedly json for %s" url)
        nil))))
