(ns braid.search.client
  (:require
   [braid.core.hooks :as hooks]))

(defonce search-results-views
  (hooks/register! (atom {}) {keyword? {:view fn?
                                        :styles vector?}}))
