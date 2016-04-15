(ns braid.ui.views.pills
  (:require [chat.client.routes :as routes]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.helpers :refer [id->color user-cursor]]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn subscribe-button-view
  [tag]
  (let [user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag (tag :id)])]
    (fn []
      (if @user-subscribed-to-tag?
        [:a.button {:on-click
                    (fn [_]
                      (dispatch! :unsubscribe-from-tag (tag :id)))}
          "Unsubscribe"]
        [:a.button {:on-click
                    (fn [_]
                      (dispatch! :subscribe-to-tag (tag :id)))}
          "Subscribe"]))))

(defn tag-pill-view
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        open-group-id (subscribe [:open-group-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag tag-id])]
    (fn []
      (let [path (routes/tag-page-path {:group-id @open-group-id
                                        :tag-id (@tag :id)})
            color (id->color (@tag :id))]
        [:a.tag.pill {:class (if @user-subscribed-to-tag? "on" "off")
                      :tabIndex -1
                      :style {:background-color color
                              :color color
                              :border-color color}
                      :href path}
          [:div.name "#" (@tag :name)]]))))


(defn user-pill-view
  [user-id]
  (let [user (subscribe [:user user-id])
        open-group-id (subscribe [:open-group-id])
        user-status (subscribe [:user-status user-id])]
    (fn []
      (let [path (routes/user-page-path {:group-id @open-group-id
                                         :user-id (@user :id)})
            color (id->color (@user :id))]
        [:a.user.pill {:class (case @user-status :online "on" "off")
                       :tabIndex -1
                       :style {:background-color color
                               :color color
                               :border-color color}
                       :href path}
          [:span.name (str "@" (@user :nickname))]
          [:div {:class (str "status " ((fnil name "") (@user :status)))}]]))))
