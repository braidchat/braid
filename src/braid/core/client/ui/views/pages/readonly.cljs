(ns braid.core.client.ui.views.pages.readonly
  (:require
   [braid.core.client.ui.views.header :refer [group-header-buttons-view]]
   [braid.core.client.ui.views.threads :refer [threads-view]]
   [braid.core.client.routes :as routes]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.ratom :include-macros true :refer-macros [reaction]]))

(defn readonly-inbox-page-view
  []
  (let [group @(subscribe [:active-group])
        group-id @(subscribe [:open-group-id])
        open-threads @(subscribe [:open-threads])]
    [:div.page.readonly-inbox
     [:div.intro
      (:intro group)]
     ^{:key group-id}
     [threads-view {:threads (map #(assoc % :readonly true) open-threads)}]]))
