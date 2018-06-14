(ns braid.core.client.ui.views.card-border
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.core.client.helpers :as helpers]))

(defn card-border-view [thread-id]
  [:div.border
   {:style
    {:background (helpers/->color @(subscribe [:open-group-id]))}}])
