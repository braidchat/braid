(ns braid.users.client.views.users-page
  (:require
   [re-frame.core :refer [subscribe]]))

(defn user-view
  [user]
  (let [nickname (:nickname user)]
    [:tr.user [:td.nickname nickname]]))

(defn users-page-view []
  (let [users @(subscribe [:users])]
    [:div.page.uploads
     [:div.title "Users"]
     [:div.content
      (cond
        (nil? users)
        [:p "Loading..."]

        (empty? users)
        [:p "No users yet"]

        :else
        [:table.users
         [:thead
          [:tr
           [:th "Nickname"]]]
         [:tbody
          (doall
           (for [user users]
             ^{:key (user :id)}
             [user-view user]))]])]]))
