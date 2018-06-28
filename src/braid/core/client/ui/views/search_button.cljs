(ns braid.core.client.ui.views.search-button
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.core.client.routes :as routes]))

(defn search-button-view [query]
  (let [open-group-id (subscribe [:open-group-id])]
    [:a.search
     {:href (routes/search-page-path {:group-id @open-group-id
                                      :query query})}
     "Search"]))
