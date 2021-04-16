(ns braid.rss.server.fetching
  "Fetching items from an RSS feed"
  (:require
   [braid.core.server.db :as db]
   [braid.base.server.cqrs :as cqrs]
   [braid.rss.server.db :as rss-db]
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
  (let [feed (some->> @(http/get url)
                      :body
                      .getBytes
                      (java.io.ByteArrayInputStream.)
                      xml/parse
                      extract-items)]
    (if (seq feed)
      feed
      (throw (ex-info "Failed to parse feed"
                      {:feed-url url})))))

(defn feed-works?
  [feed-url]
  (try
    (fetch-feed feed-url)
    true
    (catch Exception _
      false)))

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

(defn post-item
  [feed item]
  (let [thread-id (java.util.UUID/randomUUID)]
    (cqrs/dispatch
      :braid.chat/create-thread!
      {:user-id (feed :user-id)
       :thread-id thread-id
       :group-id (feed :group-id)})
    (cqrs/dispatch
      :braid.chat/create-message!
      {:message-id (java.util.UUID/randomUUID)
       ;; TODO: maybe also include description of item?
       ;; it probably is html though, which may be something not
       ;; worth the hassle
       :content (string/join "\n" [(item :title) (item :link)])
       :thread-id thread-id
       :user-id (feed :user-id)
       :mentioned-user-ids []
       :mentioned-tag-ids (vec (feed :tag-ids))})))

(defn update-feed!
  [feed]
  (let [latest (latest-item (:feed-url feed))
        guid (item-guid latest)]
    (when (not= guid (:last-fetched feed))
      (post-item feed latest)
      (db/run-txns!
        (rss-db/update-last-fetched-txn (feed :id) guid)))))
