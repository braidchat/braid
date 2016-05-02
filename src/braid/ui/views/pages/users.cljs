(ns braid.ui.views.pages.users
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.group-invite :refer [group-invite-view]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn user-view
  [user admin?]
  (let [group-id (subscribe [:open-group-id])
        user-id (r/atom (user :id))
        user-is-admin? (subscribe [:user-is-group-admin?] [user-id group-id])]
    (fn [user admin?]
      [:li [user-pill-view (user :id)]
       (when (and admin? (not @user-is-admin?))
         [:button {:on-click (fn [_]
                               (dispatch! :make-admin {:user-id (user :id)
                                                       :group-id @group-id}))}
          "Make Admin"])])))

(defn users-list-view
  [users admin?]
  [:ul
   (doall
     (for [user users]
       ^{:key (user :id)}
       [user-view user admin?]))])

(defn users-page-view
  []
  (let [online-users (subscribe [:users-in-open-group :online])
        offline-users (subscribe [:users-in-open-group :offline])
        group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.users
       [:div.title "Users"]
       [:div.content
        [:div.description
         [:h2 "Online"]
         [users-list-view @online-users @admin?]

         [:h2 "Offline"]
         [users-list-view @offline-users @admin?]]

        [:h2 "Invite"]
        [group-invite-view]]])))
