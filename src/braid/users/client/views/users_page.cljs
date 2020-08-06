(ns braid.users.client.views.users-page
  (:require
   [re-frame.core :refer [subscribe]]))

(defn user-view
  [user]
  (let [nickname (:nickname user)]
    [:tr.user
     [:td.nickname nickname]
     [:td.action [:button.make-admin
                  {:on-click
                   (fn [_])}
                  "Make Admin"]]]))

(defn users-page-view []
  (when @(subscribe [:current-user-is-group-admin?]
                    [(subscribe [:open-group-id])])
    (let [users @(subscribe [:users])]
      [:div.page.users
       [:div.title "Users"]
       [:div.content
        (cond
          (nil? users)
          [:p.users "Loading..."]

          (empty? users)
          [:p.users "No users yet"]

          :else
          [:table.users
           [:thead
            [:tr
             [:th "Nickname"]
             [:th "Actions"]]]
           [:tbody
            (doall
              (for [user users]
                ^{:key (user :id)}
                [user-view user]))]])]])))
