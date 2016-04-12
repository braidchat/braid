(ns braid.ui.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.store :as store]
            [chat.client.views.group-invite :refer [group-invite-view]]))

(defn- group-users [data]
  (->> (data :users)
       vals
       (filter (fn [u] (some #{(data :open-group-id)} (u :group-ids))))))

(defn user-view
  [user subscribe]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      [:div
        [:li
          [:a {:href (routes/user-page-path {:group-id @open-group-id
                                             :user-id (user :id)})}
            [:img.avatar {:style {:background-color (id->color (user :id))}
                          :src (user :avatar)}]
            [:p (user :nickname)]]]])))

(defn user-list-view
  [users subscribe]
  (fn []
    [:ul
      (for [user users]
        ^{:key (user :id)}
        [user-view users subscribe])]))

(defn users-page-view
  [data subscribe]
  (fn []
    [:div.page.users
      [:div.title "Users"]
      [:div.content
        (let [users-by-status (->> (group-users data)
                                   (group-by :status))]
          [:div.description
            [:h2 "Online"]
            [user-list-view (users-by-status :online) subscribe]

            [:h2 "Offline"]
            [user-list-view (users-by-status :offline) subscribe]])

        (dom/h2 nil "Invite")
        (om/build group-invite-view (data :open-group-id))]]))