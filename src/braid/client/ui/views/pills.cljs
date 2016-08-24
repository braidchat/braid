(ns braid.client.ui.views.pills
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [braid.client.routes :as routes]
            [braid.client.helpers :refer [id->color]]
            [braid.client.helpers :as helpers]))

(defn subscribe-button-view
  [tag-id]
  (let [user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    (fn [tag-id]
      (if @user-subscribed-to-tag?
        [:a.button {:on-click
                    (fn [_]
                      (dispatch [:unsubscribe-from-tag tag-id]))}
         "Unsubscribe"]
        [:a.button {:on-click
                    (fn [_]
                      (dispatch [:subscribe-to-tag {:tag-id tag-id}]))}
         "Subscribe"]))))

(defn tag-pill
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    (fn [tag-id]
      (let [color (id->color tag-id)]
        [:span.pill {:class (if @user-subscribed-to-tag? "on" "off")
                     :tabIndex -1
                     :style {:background-color color
                             :color color
                             :border-color color}}
         [:div.name "#" (@tag :name)]]))))

(defn search-button-view [query]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn [query]
      [:a.search
       {:href (routes/search-page-path {:group-id @open-group-id
                                        :query query})}
       "Search"])))

(defn tag-card-view
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    (fn [tag-id]
      [:div.card
       [:div.header {:style {:background-color (id->color tag-id)}}
        [tag-pill tag-id]
        [:div.subscribers.count
         {:title (str (@tag :subscribers-count) " Subscribers")}
         (@tag :subscribers-count)]
        [:div.threads.count
         {:title (str (@tag :threads-count) " Conversations")}
         (@tag :threads-count)]]
       [:div.info
        [:div.description
         (or (@tag :description) "If I had a description, it would be here.")]]
       [:div.actions
        [search-button-view (str "#" (@tag :name))]
        [subscribe-button-view tag-id]]])))

(defn tag-pill-view
  [tag-id]
  [:div.tag
   [tag-pill tag-id]
   [tag-card-view tag-id]])

(defn user-pill
  [user-id]
  (let [user (subscribe [:user user-id])]
    (fn [user-id]
      (let [color (id->color user-id)]
        [:span.pill {:class (str (case (@user :status) :online "on" "off"))
                     :tabIndex -1
                     :style {:background-color color
                             :color color
                             :border-color color}}
         [:span.name (str "@" (@user :nickname))]]))))

(defn user-card-view
  [user-id]
  (let [user (subscribe [:user user-id])
        open-group-id (subscribe [:open-group-id])
        admin? (subscribe [:user-is-group-admin? user-id] [open-group-id])
        viewer-admin? (subscribe [:current-user-is-group-admin?] [open-group-id])]
    (fn [user-id]
      [:div.card
       [:div.header {:style {:background-color (id->color user-id)}}
        [user-pill user-id]
        [:div.status
         (@user :status)
         ; [:div "time since last online"]
         ]
        [:div.badges
         (when @admin?
           [:div.admin {:title "admin"}])]
        [:img.avatar {:src (@user :avatar)}]]
       [:div.info
        [:div.local-time (helpers/format-date (js/Date.))]
        ; [:div.since "member since]
        [:div.description
         "If I had a profile, it would be here"]]
       [:div.actions
        ; [:a.pm "PM"]
        ; [:a.mute "Mute"]
        [search-button-view (str "@" (@user :nickname))]
        (when (and @viewer-admin? (not @admin?))
          ; TODO: make this not ugly
          [:button.make-admin
           {:on-click (fn [_] (dispatch [:make-admin {:group-id @open-group-id
                                                      :user-id user-id}]))}
           "Make Admin"])]])))

(defn user-pill-view
  [user-id]
  [:div.user
   [user-pill user-id]
   [user-card-view user-id]])
