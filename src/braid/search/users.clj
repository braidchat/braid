(ns braid.search.users
  (:require
   [datomic.api :as d]
   [braid.core.server.db :as db]))

(defn search-users-by-name
  [{:keys [query group-id]}]
  (when-let [users (->> query (re-seq #"[@]([^ ]+)") (map second) seq)]
    (->> (d/q '[:find ?u-id ?user-name
                :in $ [?user-name ...] ?g-id
                :where
                [?g :group/id ?g-id]
                [?g :group/user ?user]
                [?user :user/nickname ?user-name]
                [?user :user/id ?u-id]]
              (db/db)
              users
              group-id)
         (into #{}
               (map (fn [[user-id user-name]]
                      {:search/type :user
                       :search/sort-key user-name
                       :user-id user-id}))))))
