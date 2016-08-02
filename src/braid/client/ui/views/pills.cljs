(ns braid.client.ui.views.pills
  (:require [reagent.core :as r]
            [braid.client.routes :as routes]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.helpers :refer [id->color]]
            [braid.client.state :refer [subscribe]]))

(defn subscribe-button-view
  [tag-id]
  (let [tag-id-atom (r/atom tag-id)
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag?] [tag-id-atom])]
    (r/create-class
      {:display-name "subscribe-button-view"
       :component-will-receive-props
       (fn [_ [_ new-tag-id]]
         (reset! tag-id-atom new-tag-id))
       :reagent-render
       (fn [tag-id]
         (if @user-subscribed-to-tag?
           [:a.button {:on-click
                       (fn [_]
                         (dispatch! :unsubscribe-from-tag tag-id))}
            "Unsubscribe"]
           [:a.button {:on-click
                       (fn [_]
                         (dispatch! :subscribe-to-tag {:tag-id tag-id}))}
            "Subscribe"]))})))

(defn tag-pill-view
  [tag-id]
  (let [tag-id-atom (r/atom tag-id)
        tag (subscribe [:tag] [tag-id-atom])
        open-group-id (subscribe [:open-group-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag?] [tag-id-atom])]
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
                         :href path
                         :title (@tag :description)}
            [:div.name "#" (@tag :name)]]))})))


(defn user-pill-view
  [user-id]
  (let [user-id-atom (r/atom user-id)
        user (subscribe [:user] [user-id-atom])
        open-group-id (subscribe [:open-group-id])
        admin? (subscribe [:user-is-group-admin?] [user-id-atom open-group-id])
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
           [:a.user.pill {:class (str (case @user-status :online "on" "off")
                                      (when @admin? " admin"))
                          :title (when @admin? "admin")
                          :tabIndex -1
                          :style {:background-color color
                                  :color color
                                  :border-color color}
                          :href path}
            [:span.name (str "@" (@user :nickname))]
            [:div {:class (str "status " ((fnil name "") (@user :status)))}]]))})))
