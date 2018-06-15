(ns braid.core.client.ui.views.header-item
  (:require
    [re-frame.core :refer [subscribe]]
    [spec-tools.data-spec :as ds]))

(def HeaderItem
  {(ds/opt :title) string?
   :priority number?
   (ds/opt :icon) string?
   (ds/opt :on-click) fn?
   (ds/opt :route-fn) fn?
   (ds/opt :route-args) {keyword? any?}
   (ds/opt :body) string?})

(defn header-item-view
  [conf]
  (let [open-group-id (subscribe [:open-group-id])
        current-path (subscribe [:page-path])]
    (fn [{:keys [route-fn route-args title icon body on-click]}]
      (let [path (when route-fn
                   (route-fn (merge route-args {:group-id @open-group-id})))]
        [:a {:class (when (= path @current-path) "active")
             :href path
             :on-click on-click
             :title title}
         body
         (when icon
           [:div.icon icon])]))))
