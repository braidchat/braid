(ns braid.core.client.ui.views.search-button
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.core.client.routes :as routes]))

(defn search-button-view [query]
  (let [open-group-id (subscribe [:open-group-id])]
    [:a.search
     {:href (routes/group-page-path {:group-id @open-group-id
                                     :page-id "search"
                                     :query-params {:query query}})}
     "Search"]))
