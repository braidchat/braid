(ns braid.server.search.elasticsearch
  (:require
    [braid.server.conf :refer [config]]
    [braid.server.db :as db]
    [clojure.data.json :as json]
    [org.httpkit.client :as http]))

(defn elasticsearch-enabled? []
  (some? (config :elasticsearch-url)))

(defn search-for
  [{:keys [text group-id user-id]}]
  (some->
    @(http/get (str (config :elasticsearch-url)
                    "/braid-messages/_search")
       {:body (json/write-str {:query {:match {:content text}}})})
    :body
    json/read-str
    (get-in ["hits" "hits"])
    (->>
      (into #{}
            (comp
              (map #(get-in % ["_source" "thread-id"]))
              (map #(java.util.UUID/fromString %))
              (filter #(= group-id (db/thread-group-id %)))
              (map (fn [t-id] [t-id (db/thread-newest-message t-id)])))))
    seq
    set))
