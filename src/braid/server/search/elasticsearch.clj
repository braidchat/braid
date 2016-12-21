(ns braid.server.search.elasticsearch
  (:require
    [braid.server.conf :refer [config]]
    [braid.server.db :as db]
    [braid.server.db.thread :as thread]
    [clojure.data.json :as json]
    [datomic.api :as d]
    [org.httpkit.client :as http]))

(defn elasticsearch-enabled? []
  (some? (config :elasticsearch-url)))

(defn search-for
  [{:keys [text group-id user-id]}]
  (let [resp (->
               @(http/get (str (config :elasticsearch-url)
                               ; TODO: page results properly
                               "/braid-messages/_search?size=500")
                  {:body (json/write-str {:query {:match {:content text}}})})
               :body
               json/read-str
               (get "hits"))]
    (->> (get resp "hits")
         (map #(Long. (get % "_id")))
         (d/pull-many (d/db db/conn) [{:message/thread [:thread/id]}])
         (into #{}
               (comp
                 (map #(get-in % [:message/thread :thread/id]))
                 (filter #(= group-id (thread/thread-group-id db/conn %)))
                 (map (fn [t-id] [t-id (thread/thread-newest-message db/conn t-id)]))))
         seq
         set)))
