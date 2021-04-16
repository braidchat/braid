(ns braid.search.api
  (:require
   #?@(:clj
       [[braid.search.server :as search]]
       :cljs
       [[braid.search.client :as search]])))

#?(:clj
   (do
     (defn register-search-function!
       "Register a function to include search results.

        The function will be called with a map with the following keys:
        - `:user-id` the id of the user doing the search
        - `:group-id` the id of the group in which the search is taking place
        - `:query` the search string

        The function should return a map with the following keys:
        - `:search/type` the type of entity being returned (see `register-search-auth-check!`)
        - `:search/sort-key` value to sort results by
        - any other keys which will be used to display search results
  "
       [search-fn]
       (swap! search/search-functions conj search-fn))

     (defn register-search-auth-check!
       "Register a function to perform authorization filtering for
     search results. It will be called with the id of the user
     performing the search and a collection of search results of the
     type `entity-type` (see `register-search-function!`)"
       [entity-type auth-fn]
       (swap! search/search-type-auth-check assoc entity-type auth-fn)))

   :cljs
   (do
     (defn register-search-results-view!
       "Register a view and styles to display the search results for a search type.

        The view function will recieve two arguments, the `status` of
        the view page (which can be `:error`, `:loading`,
        `:searching`, `:done-results`, or `:done-empty`) and the
        results for the registered type."
       [entity-type {:keys [view styles]}]
       (swap! search/search-results-views assoc entity-type {:view view :styles styles})))

   )
