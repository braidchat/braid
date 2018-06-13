(ns braid.rss.core
  "Extension to post updates from RSS feeds as messages in a given group"
  (:require
   [braid.core.core :as core]
   #?@(:clj
       [[braid.core.server.db :as db]
        [clj-time.format :as f]
        [clojure.string :as string]
        [clojure.xml :as xml]
        [clojure.walk :as walk]
        [datomic.api :as d]
        [datomic.db]
        [org.httpkit.client :as http]])))

(defn parse-item
  [item]
  (-> (reduce (fn [m {:keys [tag content]}]
                (assoc m tag (string/join "" content)))
              {} (:content item))
      (update :pubDate (partial f/parse (f/formatters :rfc822)))
      (update :description string/trim)))

(defn extract-items
  [xml]
  (let [items (atom [])]
    (walk/postwalk (fn [form]
                     (if (and (map? form) (= (:tag form) :item))
                       (do (swap! items conj (parse-item form))
                           nil)
                       form))
                   xml)
    @items))

(defn fetch-feed
  [url]
  (->> @(http/get url)
      :body
      .getBytes
      (java.io.ByteArrayInputStream.)
      xml/parse
      extract-items))

(defn item-guid
  [item]
  (or (:guid item)
      (->> ((apply juxt (sort (keys item))) item)
          (string/join "")
          hash str)))

(defn update-feed!
  [feed-url]
  (->> (fetch-feed feed-url)
      (sort-by :pubDate #(compare %2 %1))
      first))

(defn post-item
  [feed item]
  )

(defn init!
  []
  #?(:clj
     (do
       (core/register-db-schema!
         [{:db/ident :rss/id
           :db/valueType :db.type/uuid
           :db/doc "Identifer of the RSS feed"
           :db/cardinality :db.cardinality/one
           :db/unique :db.unique/identity
           :db/id #db/id[:db.part/db]
           :db.install/_attribute :db.part/db}
          {:db/ident :rss/feed-url
           :db/valueType :db.type/string
           :db/doc "URL of the feed to fetch"
           :db/cardinality :db.cardinality/one
           :db/id #db/id[:db.part/db]
           :db.install/_attribute :db.part/db}
          {:db/ident :rss/last-fetched
           :db/valueType :db.type/string
           :db/doc "Unique identifier of the last fetched feed item. Either the item <guid> or a hash of the content."
           :db/cardinality :db.cardinality/one
           :db/id #db/id[:db.part/db]
           :db.install/_attribute :db.part/db}
          {:db/ident :rss/group
           :db/valueType :db.type/ref
           :db/doc "The group the feed entries will be posted to"
           :db/cardinality :db.cardinality/one
           :db/db #db/id[:db.part/db]
           :db.install/_attribute :db.part/db}
          {:db/ident :rss/post-as
           :db/valueType :db.type/ref
           :db/cardinality :db.cardinality/one
           :db/doc "The user which the items will be posted as"
           :db/id #db/id[:db.part/db]
           :db.install/_attribute}
          {:db/ident :rss/post-tag
           :db/valueType :db.type/ref
           :db/cardinality :db.cardinality/many
           :db/doc "Tags which will be applied to feed items"
           :db/id #db/id[:db.part/db]
           :db.install/_attribute}]))))
