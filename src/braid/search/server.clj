(ns braid.search.server
  (:require
    [clojure.set :as set]
    [braid.core.hooks :as hooks]))

(defonce search-functions
  (hooks/register! (atom []) [fn?]))

(defonce search-type-auth-check
  (hooks/register! (atom {}) {keyword? fn?}))

(defn- identity-filter
  [_ results]
  results)

(defn tee [x] (prn x) x)

(defn search-as
  "Return the ids of all threads, visible to the user, in the given group,
  matching the provided query.
  The query can specify tags by prefixing them with an octothorpe; for example,
  the query 'foo #bar' will find any threads tagged with 'bar' containing the
  text 'foo'"
  [user-id [query group-id]]
  ; TODO: pagination?
  (let [results (pmap (fn [search-fn]
                        (search-fn {:user-id user-id
                                    :query query
                                    :group-id group-id}))
                      @search-functions)]
    (->> (remove nil? results)
         ;; group the results by type, without merging all the results
         ;; together, so we can take the intersection of the multiple
         ;; search functions for a given type
         (reduce
           (fn [m set-of-maps]
             (->> (group-by :search/type set-of-maps)
                  (reduce (fn [m [type results]]
                            (update m type (fnil conj []) (set results)))
                          m)))
           {})
         (reduce
           (fn [m [type results]]
             (assoc m type ((get @search-type-auth-check type identity-filter)
                            user-id
                            (apply set/intersection results))))
           {}))))
