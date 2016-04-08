(ns braid.api.embedly
  (:require [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [clojure.data.json :as json]))

(defn- get-json [url]
  (->
    @(http/request {:url "https://api.embedly.com/1/extract"
                    :query-params {"key" (env :embedly-key)
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
  (when (env :embedly-key)
    (get-json-memo url)))
