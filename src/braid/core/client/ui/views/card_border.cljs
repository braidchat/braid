(ns braid.core.client.ui.views.card-border
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.lib.color :as color]))

(defn card-border-view [thread-id]
  [:div.border
   {:style
    {:background (color/->color @(subscribe [:open-group-id]))}}])
