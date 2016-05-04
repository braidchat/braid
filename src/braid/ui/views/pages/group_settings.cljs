(ns braid.ui.views.pages.group-settings
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn intro-message-view
  [group]
  (let [new-message (r/atom "")]
    (fn [group]
      [:div.setting
       [:h2 "Intro Message"]
       [:p "Current intro message"]
       [:blockquote (:intro group)]
       [:p "Set new intro"]
       [:textarea {:placeholder "New message"
                   :value @new-message
                   :on-change (fn [e] (reset! new-message (.. e -target -value)))}]
       [:button {:on-click (fn [_]
                             (dispatch! :set-intro {:group-id (group :id)
                                                    :intro @new-message})
                             (reset! new-message ""))}
        "Save"]])))

(defn group-settings-view
  []
  (let [group (subscribe [:active-group])
        group-id (reaction (:id @group))
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.settings
       (if (not @admin?)
         [:h1 "Permission Denied"]
         [:div
          [:h1 (str "Settings for " (:name @group))]
          [intro-message-view @group]])])))
