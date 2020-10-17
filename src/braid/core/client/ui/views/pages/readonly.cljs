(ns braid.core.client.ui.views.pages.readonly
  (:require
   [braid.core.client.ui.views.header :refer [group-header-buttons-view]]
   [braid.core.client.ui.views.threads :refer [threads-view]]
   [braid.core.client.routes :as routes]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.ratom :include-macros true :refer-macros [reaction]]))

(defn readonly-inbox-page-view
  []
  (let [group (subscribe [:active-group])
        group-id (subscribe [:open-group-id])
        open-threads (subscribe [:open-threads] [group-id])]
    [:div.page.readonly-inbox
     [:div.intro
      (:intro @group)]
     [threads-view {:show-new-thread? false
                    :group-id @group-id
                    :threads (map #(assoc % :readonly true) @open-threads)}]]))
