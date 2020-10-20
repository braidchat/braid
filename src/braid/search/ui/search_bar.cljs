(ns braid.search.ui.search-bar
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [braid.core.client.routes :as routes]
    [braid.lib.color :as color]))

(defn search-bar-view
  []
  (let [search-query @(subscribe [:braid.search/query])]
    [:div.search-bar
     [:input {:type "text"
              :placeholder "Search..."
              :value search-query
              :on-change
              (fn [e]
                (dispatch [:braid.search/update-query! (.. e -target -value)]))}]
     (if search-query
       [:a.action.clear
        {:href (routes/group-page-path {:group-id @(subscribe [:open-group-id])
                                        :page-id "inbox"})
         :style {:color (color/->color @(subscribe [:open-group-id]))}}]
       [:div.action.search])]))
