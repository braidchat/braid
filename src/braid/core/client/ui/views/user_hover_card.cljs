(ns braid.core.client.ui.views.user-hover-card
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.search.ui.search-button :refer [search-button-view]]
    [braid.core.client.ui.views.pills :refer [user-pill-view]]
    [braid.lib.color :as color]
    [braid.lib.date :as date]
    [braid.lib.upload :as upload]))

(defn user-hover-card-view
  [user-id]
  (if-let [user @(subscribe [:user user-id])]
    (let [open-group-id (subscribe [:open-group-id])
          admin? (subscribe [:user-is-group-admin? user-id] [open-group-id])
          viewer-admin? (subscribe [:current-user-is-group-admin?] [open-group-id])]
      [:div.user.card

       [:div.header {:style {:background-color (color/->color user-id)}}
        [user-pill-view user-id]
        [:div.status
         (user :status)
         #_[:div "time since last online"]]
        [:div.badges
         (when @admin?
           [:div.admin {:title "admin"}])]
        [:img.avatar {:src (upload/->path (user :avatar))}]]

       [:div.info
        [:div.local-time (date/smart-format-date (js/Date.))]
        [:div.since
         "member since " (date/smart-format-date (user :joined-at))]
        [:div.description
         #_"If I had a profile, it would be here"]]

       [:div.actions

          ; Open a new message with the selected user on click
          [:a.pm
            {:on-click
              (fn [_]
                (dispatch [:new-conversation
                           {:group-id @open-group-id
                            :mentioned-user-ids [user-id]}]))}
            "PM"]
        #_[:a.mute "Mute"]

        [search-button-view (str "@" (user :nickname))]

        (when (and @viewer-admin? (not= user-id @(subscribe [:user-id])))
          [:button.ban
           {:on-click
            (fn [_]
              (when (js/confirm (str "Really remove " (user :nickname) " from this group?"))
                (dispatch [:remove-from-group!
                           {:group-id @open-group-id
                            :user-id user-id}])))}
           "Kick"])

        (when (and @viewer-admin? (not @admin?))
          [:button.make-admin
           {:on-click
            (fn [_]
              (dispatch [:make-admin!
                         {:group-id @open-group-id
                          :user-id user-id}]))}
           "Make Admin"])]])

    [:div.user.card
     [:div.header {:style {:background-color (color/->color user-id)}}
      [user-pill-view user-id]]
     [:div.info "No longer in group"]]))
