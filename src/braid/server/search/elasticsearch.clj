(ns braid.server.search.elasticsearch
  (:require
    [clojure.data.json :as json]
    [environ.core :refer [env]]
    [org.httpkit.client :as http]))

(defn search-for
  [text]
  (let [resp (-> @(http/get (str (env :elasticsearch-url) "/braid-messages/_search")
                    {:body (json/write-str {:query {:match {:content text}}})})
                 :body
                 json/read-str)]
    (when (get resp "hits")
      (map #(java.util.UUID/fromString (get-in % ["_source" "thread-id"]))
           (get-in resp ["hits" "hits"])))))
