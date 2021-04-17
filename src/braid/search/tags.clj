(ns braid.search.tags
  (:require
   [datomic.api :as d]
   [braid.core.server.db :as db]))

(defn search-tags-by-name
  [{:keys [query group-id]}]
  (when-let [tags (->> query (re-seq #"[#]([^ ]+)") (map second) seq)]
    (->> (d/q '[:find ?t-id ?tag-name
                :in $ [?tag-name ...] ?g-id
                :where
                [?g :group/id ?g-id]
                [?t :tag/name ?tag-name]
                [?t :tag/group ?g]
                [?t :tag/id ?t-id]]
              (db/db)
              tags
              group-id)
         (into #{}
               (map (fn [[tag-id tag-name]]
                      {:search/type :tag
                       :search/sort-key tag-name
                       :tag-id tag-id}))))))
