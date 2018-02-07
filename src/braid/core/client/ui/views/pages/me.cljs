(ns braid.core.client.ui.views.pages.me
  (:require
   [braid.core.client.routes :as routes]
   [re-frame.core :refer [dispatch subscribe]]))

(defn leave-group-view
  []
  (let [group (subscribe [:active-group])
        user-id (subscribe [:user-id])]
    (fn []
      [:button {:on-click (fn [_]
                            (when (js/confirm (str "Are you sure you want to leave" (@group :name) "?"))
                              (dispatch [:remove-from-group
                                         {:group-id (@group :id)
                                          :user-id @user-id}])))}
       (str "Leave " (@group :name))])))

(defn me-page-view
  []
  [:div.page.me
   [:div.title "Me!"]
   [:div.content
    [:p "Placeholder page for group-related profile settings"]
    [leave-group-view]
    [:p
     [:a {:href (routes/other-path {:page-id "global-settings"})}
      "Go to Global Settings"]]]])
