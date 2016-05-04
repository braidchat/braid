(ns braid.ui.views.pages.group-settings
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            ))

(defn group-settings-view
  []
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.settings
       (if @admin?
         [:h2 (str "Settings for " @group-id)]
         [:h2 "No"])])))
