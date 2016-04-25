(ns braid.ui.views.pills
  (:require [reagent.core :as r]
            [reagent.impl.util :refer [extract-props]]
            [chat.client.routes :as routes]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn subscribe-button-view
  [tag-id]
  (let [user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag tag-id])]
    (fn []
      (if @user-subscribed-to-tag?
        [:a.button {:on-click
                    (fn [_]
                      (dispatch! :unsubscribe-from-tag tag-id))}
          "Unsubscribe"]
        [:a.button {:on-click
                    (fn [_]
                      (dispatch! :subscribe-to-tag tag-id))}
          "Subscribe"]))))

(defn tag-pill-view
  [tag-id]
  (let [tag-id-atom (r/atom tag-id)
        tag (subscribe [:tag] [tag-id-atom])
        open-group-id (subscribe [:open-group-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag] [tag-id-atom])]
    (r/create-class
      {:display-name "tag-pill-view"
       :component-will-receive-props
       (fn [_ [_ new-tag-id]]
         (reset! tag-id-atom new-tag-id))
       :reagent-render
       (fn [tag-id]
         (let [path (routes/tag-page-path {:group-id @open-group-id
                                           :tag-id (@tag :id)})
               color (id->color (@tag :id))]
           [:a.tag.pill {:class (if @user-subscribed-to-tag? "on" "off")
                         :tabIndex -1
                         :style {:background-color color
                                 :color color
                                 :border-color color}
                         :href path}
            [:div.name "#" (@tag :name)]]))})))


(defn user-pill-view
  [user-id]
  (let [user-id-atom (r/atom user-id)
        user (subscribe [:user] [user-id-atom])
        open-group-id (subscribe [:open-group-id])
        user-status (subscribe [:user-status] [user-id-atom])]
    (r/create-class
      {:display-name "user-pill-view"
       :component-will-receive-props
       (fn [_ [_ new-user-id]]
         (reset! user-id-atom new-user-id))

       :reagent-render
       (fn [user-id]
         (let [path (routes/user-page-path {:group-id @open-group-id
                                            :user-id user-id})
               color (id->color user-id)]
           [:a.user.pill {:class (case @user-status :online "on" "off")
                          :tabIndex -1
                          :style {:background-color color
                                  :color color
                                  :border-color color}
                          :href path}
            [:span.name (str "@" (@user :nickname))]
            [:div {:class (str "status " ((fnil name "") (@user :status)))}]]))})))
