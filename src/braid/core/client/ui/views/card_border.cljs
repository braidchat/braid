(ns braid.core.client.ui.views.card-border
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.core.client.helpers :as helpers]))

(defn card-border-view [thread-id]
  (let [color (helpers/->color @(subscribe [:open-group-id]))]
    [:div.border
     {:style
      {:background (if @(subscribe [:thread-focused? thread-id])
                     (helpers/darken color)
                     color)}}]))
