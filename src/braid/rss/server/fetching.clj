(ns braid.rss.server.fetching
  "Fetching items from an RSS feed"
  (:require
   [braid.core.server.db :as db]
   [braid.core.server.db.message :as message]
   [braid.core.server.sync-helpers :as sync-helpers]
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
  (let [msg {:id (java.util.UUID/randomUUID)
             ;; TODO: maybe also include description of item?
             ;; it probably is html though, which may be something not
             ;; worth the hassle
             :content (string/join "\n" [(item :title) (item :link)])
             :thread-id (java.util.UUID/randomUUID)
             :user-id (feed :user-id)
             :group-id (feed :group-id)
             :created-at (java.util.Date.)
             :mentioned-user-ids []
             :mentioned-tag-ids (feed :tag-ids)}]
    (db/run-txns! (message/create-message-txn msg))
    ;; NB: explicitly *not* running sync-helpers/notify-users here
    ;; since this is a sort of auto-generated post, it seems to
    ;; not make the most sense
    (sync-helpers/broadcast-thread (msg :thread-id) [])))

(defn update-feed!
  [feed]
  (let [latest (latest-item (:feed-url feed))
        guid (item-guid latest)]
    (when (not= guid (:last-fetched feed))
      (post-item feed latest)
      (db/run-txns!
        (rss-db/update-last-fetched-txn (feed :id) guid)))))
