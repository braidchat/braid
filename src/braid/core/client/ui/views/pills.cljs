(ns braid.core.client.ui.views.pills
  (:require
    [braid.popovers.helpers :as popover]
    [braid.core.client.helpers :as helpers]
    [braid.core.client.helpers :refer [->color]]
    [braid.core.client.routes :as routes]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn subscribe-button-view
  [tag-id]
  (let [user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    (if @user-subscribed-to-tag?
      [:a.button {:on-click
                  (fn [_]
                    (dispatch [:unsubscribe-from-tag tag-id]))}
       "Unsubscribe"]
      [:a.button {:on-click
                  (fn [_]
                    (dispatch [:subscribe-to-tag {:tag-id tag-id}]))}
       "Subscribe"])))

(defn tag-pill
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])
        color (->color tag-id)]
    [:span.pill {:class (if @user-subscribed-to-tag? "on" "off")
                 :tab-index -1
                 :style {:background-color color
                         :color color
                         :border-color color}}
     [:div.name "#" (@tag :name)]]))

(defn search-button-view [query]
  (let [open-group-id (subscribe [:open-group-id])]
    [:a.search
     {:href (routes/search-page-path {:group-id @open-group-id
                                      :query query})}
     "Search"]))

(defn tag-card-view
  [tag-id]
  (let [tag (subscribe [:tag tag-id])
        user-subscribed-to-tag? (subscribe [:user-subscribed-to-tag? tag-id])]
    [:div.tag.card
     [:div.header {:style {:background-color (->color tag-id)}}
      [tag-pill tag-id]
      [:div.subscribers.count
       {:title (str (@tag :subscribers-count) " Subscribers")}
       (@tag :subscribers-count)]
      [:div.threads.count
       {:title (str (@tag :threads-count) " Conversations")}
       (@tag :threads-count)]]
     [:div.info
      [:div.description
       (@tag :description)]]
     [:div.actions
      [search-button-view (str "#" (@tag :name))]
      [subscribe-button-view tag-id]]]))

(defn tag-pill-view
  [tag-id]
  [:div.tag
   {:style {:display "inline-block"}
    :on-mouse-enter
    (popover/on-mouse-enter
      (fn []
        [tag-card-view tag-id]))}
   [tag-pill tag-id]])

(defn user-pill
  [user-id]
  (let [user (subscribe [:user user-id])]
    (let [color (->color user-id)]
      [:span.pill {:class (str (case (@user :status) :online "on" "off"))
                   :tab-index -1
                   :style {:background-color color
                           :color color
                           :border-color color}}
       [:span.name (str "@" (@user :nickname))]])))

(defn user-card-view
  [user-id]
  (let [user (subscribe [:user user-id])
        open-group-id (subscribe [:open-group-id])
        admin? (subscribe [:user-is-group-admin? user-id] [open-group-id])
        viewer-admin? (subscribe [:current-user-is-group-admin?] [open-group-id])]
    [:div.user.card

     [:div.header {:style {:background-color (->color user-id)}}
      [user-pill user-id]
      [:div.status
       (@user :status)
       #_[:div "time since last online"]]
      [:div.badges
       (when @admin?
         [:div.admin {:title "admin"}])]
      [:img.avatar {:src (@user :avatar)}]]

     [:div.info
      [:div.local-time (helpers/format-date (js/Date.))]
      #_[:div.since "member since"]
      [:div.description
       #_"If I had a profile, it would be here"]]

     [:div.actions
      #_[:a.pm "PM"]
      #_[:a.mute "Mute"]

      [search-button-view (str "@" (@user :nickname))]

      (when (and @viewer-admin? (not= user-id @(subscribe [:user-id])))
        [:button.ban
         {:on-click
          (fn [_]
            (dispatch [:remove-from-group
                       {:group-id @open-group-id
                        :user-id user-id}]))}
         "Kick"])

      (when (and @viewer-admin? (not @admin?))
        [:button.make-admin
         {:on-click
          (fn [_]
            (dispatch [:make-admin
                       {:group-id @open-group-id
                        :user-id user-id}]))}
         "Make Admin"])]]))

(defn user-pill-view
  [user-id]
  [:div.user
   {:style {:display "inline-block"}
    :on-mouse-enter
    (popover/on-mouse-enter
      (fn []
        [user-card-view user-id]))}
   [user-pill user-id]])
