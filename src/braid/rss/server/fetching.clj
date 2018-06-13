(ns braid.rss.server.fetching
  "Fetching items from an RSS feed"
  (:require
   [clj-time.format :as f]
   [clojure.string :as string]
   [clojure.xml :as xml]
   [clojure.walk :as walk]
   [org.httpkit.client :as http]))

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

(defn latest-item
  [feed-url]
  (->> (fetch-feed feed-url)
      (sort-by :pubDate #(compare %2 %1))
      first))
