(ns braid.search.threads
  (:require
   [clojure.string :as string]
   [datomic.api :as d]
   [braid.core.server.db :as db]
   [braid.search.lucene :as lucene])
  (:import
   (org.apache.lucene.index IndexNotFoundException)))

(defn search-threads-by-tag
  [{:keys [query group-id]}]
  (when-let [tags (->> query (re-seq #"[#]([^ ]+)") (map second) seq)]
    (->> (d/q '[:find ?t-id (max ?time)
                :in $ [?tag-name ...] ?g-id
                :where
                ;; [TODO] be more flexible/allow partial match?
                [?g :group/id ?g-id]
                [?tag :tag/group ?g]
                [?tag :tag/name ?tag-name]
                [?t :thread/tag ?tag]
                [?t :thread/id ?t-id]
                [?m :message/thread ?t]
                [?m :message/created-at ?time]]
              (db/db)
              tags
              group-id)
         (into #{}
               (map (fn [[thread-id last-update]]
                      {:search/type :thread
                       :search/sort-key last-update
                       :thread-id thread-id}))))))

(defn search-threads-by-user
  [{:keys [query group-id]}]
  (when-let [users (->> query (re-seq #"[@]([^ ]+)") (map second) seq)]
    (->> (d/q '[:find ?t-id (max ?time)
                :in $ [?user-name ...] ?g-id
                :where
                [?g :group/id ?g-id]
                [?t :thread/group ?g]
                [?t :thread/id ?t-id]
                (or (and [?t :thread/mentioned ?user]
                         [?m :message/thread ?t])
                    (and [?m :message/thread ?t]
                         [?m :message/user ?user]))
                [?g :group/user ?user]
                [?user :user/nickname ?user-name]
                [?m :message/created-at ?time]]
              (db/db)
              users
              group-id)
         (into #{}
               (map (fn [[thread-id last-update]]
                      {:search/type :thread
                       :search/sort-key last-update
                       :thread-id thread-id}))))))

(defn search-threads-by-full-text
  [{:keys [query group-id]}]
  (when-not (string/blank? query)
    (->> (try (lucene/search group-id query)
              (catch IndexNotFoundException _ nil))
        (into #{}
              (map (fn [[thread-id last-update]]
                     {:search/type :thread
                      :search/sort-key last-update
                      :thread-id thread-id}))))))
