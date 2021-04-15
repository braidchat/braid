(ns braid.search.api
  (:require
   [braid.base.api :as base]
   #?@(:clj
       [[braid.search.server :as search]])))

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

   )
