(ns braid.ui.views.pages.users
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.group-invite :refer [group-invite-view]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.store :as store]))

(defn user-list-view
  [users]
  (fn []
    [:ul
      (for [user users]
        ^{:key (user :id)}
        [:li [user-pill-view user]])]))

(defn users-page-view
  []
  (let [users (subscribe [:users-in-open-group])]
    (fn []
      [:div.page.users
        [:div.title "Users"]
        [:div.content
          (let [users-by-status (->> @users
                                     (group-by :status))]
            [:div.description
              [:h2 "Online"]
              [user-list-view (users-by-status :online)]

              [:h2 "Offline"]
              [user-list-view (users-by-status :offline)]])

          [:h2 "Invite"]
          [group-invite-view]]])))
