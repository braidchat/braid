(ns braid.core.client.ui.views.card-border
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.core.client.helpers :as helpers]))

(defn card-border-view [thread-id]
  (let [group-id @(subscribe [:open-group-id])
        color (helpers/->color group-id)]
    [:div.border
     {:style
      {:background (if @(subscribe [:thread-focused? thread-id])
                     (helpers/darken color)
                     (helpers/->color color))}}]))
