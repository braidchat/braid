(ns braid.core.client.ui.views.header-item
  (:require
    [re-frame.core :refer [subscribe]]))

(defn header-item-view
  [conf]
  (let [open-group-id (subscribe [:open-group-id])
        current-path (subscribe [:page-path])]
    ; TODO: reaction for path = current-path? Would close over conf, probably
    ; not worthwhile
    (fn [{:keys [route-fn route-args title class icon body]}]
      (let [path (route-fn (merge route-args {:group-id @open-group-id}))]
        [:a {:class (str class (when (= path @current-path) " active"))
             :href path
             :title title}
         (when icon
           [:div.icon icon])
         body]))))
