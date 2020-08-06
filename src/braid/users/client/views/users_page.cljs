(ns braid.users.client.views.users-page
  (:require
   [re-frame.core :refer [dispatch subscribe]]))

(defn user-view
  [{:keys [nickname id] :as user}]
  (let [group-id (subscribe [:open-group-id])]
    [:tr.user
     [:td.nickname nickname]
     [:td.action
      (if @(subscribe [:user-is-group-admin? id] [group-id])
        [:span.admin "Admin"]
        [:button.make-admin
         {:on-click (fn [_]
                      (dispatch [:make-admin
                                 {:group-id @group-id
                                  :user-id id}]))}
         "Make Admin"])
      (when (not= id @(subscribe [:user-id]))
        [:button.ban {:on-click (fn []
                                  (dispatch [:remove-from-group
                                             {:group-id @group-id
                                              :user-id id}]))}
         "Kick from Group"])]]))

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
