(ns braid.search.ui.search-button
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.core.client.routes :as routes]))

(defn search-button-view [query]
  [:a.search
   {:href (routes/group-page-path {:group-id @(subscribe [:open-group-id])
                                   :page-id "search"
                                   :query-params {:query query}})}
   "Search"])
