(ns braid.core.client.ui.views.tag-hover-card
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.core.client.helpers :as helpers]
    [braid.core.client.ui.views.search-button :refer [search-button-view]]
    [braid.core.client.ui.views.subscribe-button :refer [subscribe-button-view]]
    [braid.core.client.ui.views.pills :refer [tag-pill-view]]))

(defn tag-hover-card-view
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    [:div.tag.card
     [:div.header {:style {:background-color (helpers/->color tag-id)}}
      [tag-pill-view tag-id]
      [:div.subscribers.count
       {:title (str (@tag :subscribers-count) " Subscribers")}
       (@tag :subscribers-count)]
      [:div.threads.count
       {:title (str (@tag :threads-count) " Conversations")}
       (@tag :threads-count)]]
     [:div.info
      [:div.description
       (@tag :description)]]
     [:div.actions
      [search-button-view (str "#" (@tag :name))]
      [subscribe-button-view tag-id]]]))
