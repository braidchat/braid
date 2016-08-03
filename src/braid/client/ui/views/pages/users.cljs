(ns braid.client.ui.views.pages.users
  (:require [reagent.core :as r]
            [braid.client.state :refer [subscribe]]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.dispatcher :refer [dispatch!]]))

(defn user-view
  [user admin?]
  (let [group-id (subscribe [:open-group-id])
        user-id (r/atom (user :id))
        user-is-admin? (subscribe [:user-is-group-admin?] [user-id group-id])
        current-user-id (subscribe [:user-id])]
    (fn [user admin?]
      [:li [user-pill-view (user :id)]
       (when (and admin? (not @user-is-admin?))
         [:button {:on-click (fn [_]
                               (dispatch! :make-admin {:user-id (user :id)
                                                       :group-id @group-id}))}
          "Make Admin"])
       (when admin?
         [:button {:on-click (fn [_]
                               (when (js/confirm "Remove user from this group?")
                                 (dispatch! :remove-from-group
                                            {:group-id @group-id
                                             :user-id (user :id)})))}
          "Remove From Group"])])))

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
         [users-list-view @offline-users @admin?]]]])))
