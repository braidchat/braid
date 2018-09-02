(ns braid.search.elasticsearch
  (:require
    [clojure.data.json :as json]
    [datomic.api :as d]
    [org.httpkit.client :as http]
    [braid.core.server.conf :refer [config]]
    [braid.core.server.db :as db]
    [braid.core.server.db.thread :as thread]))

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
         (d/pull-many (db/db) [{:message/thread [:thread/id]}])
         (into #{}
               (comp
                 (map #(get-in % [:message/thread :thread/id]))
                 (filter #(= group-id (thread/thread-group-id %)))
                 (map (fn [t-id] [t-id (thread/thread-newest-message t-id)]))))
         seq
         set)))
