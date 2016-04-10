(ns braid.ui.views.pills
  (:require [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [id->color user-cursor]]))

(defn tag-pill-view
  [tag subscribe]
  (let [open-group-id (subscribe [:open-group-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag (tag :id)])]
    (fn []
      (let [path (routes/tag-page-path {:group-id @open-group-id
                                        :tag-id (tag :id)})]
        [:a.tag.pill {:class (if @user-subscribed-to-tag?
                                " on"
                                " off")
                      :tabIndex -1
                      :style {:background-color (id->color (tag :id))
                              :color (id->color (tag :id))
                              :borderColor (id->color (tag :id))}
                      :href path}
          [:div.name "#" (tag :name)]]))))


(defn user-pill-view
  [user subscribe]
  (let [open-group-id (subscribe [:open-group-id])
        user-status (subscribe [:user-status (user :id)])]
    (fn []
      (let [path (routes/user-page-path {:group-id @open-group-id
                                         :user-id (user :id)})]
        [:a.user.pill {:class (case @user-status
                                              :online " on"
                                                      " off")
                       :tabIndex -1
                       :style {:background-color (id->color (user :id))
                               :color (id->color (user :id))
                               :borderColor (id->color (user :id))}
                       :href path}
          [:span.name (str "@" (user :nickname))]
          [:div {:class (str "status " ((fnil name "") (user :status)))}]]))))
