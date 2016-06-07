(ns braid.ui.views.header
  (:require [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.pills :refer [tag-pill-view user-pill-view]]
            [braid.ui.views.search-bar :refer [search-bar-view]]))

(defn clear-inbox-button-view []
  (let [group-id (subscribe [:open-group-id])
        open-threads (subscribe [:open-threads] [group-id])]
    (fn []
      [:div.clear-inbox
        (when (< 5 (count @open-threads))
          [:button {:on-click (fn [_]
                                (dispatch! :clear-inbox))}
            "Clear Inbox"])])))

(defn inbox-page-button-view []
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/inbox-page-path {:group-id @open-group-id})]
        [:div.inbox.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Inbox"}]]))))

(defn recent-page-button-view []
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/recent-page-path {:group-id @open-group-id})]
        [:div.recent.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Recent"}]]))))

(defn users-online-pane-view []
  (let [open-group-id (subscribe [:open-group-id])
        user-id (subscribe [:user-id])
        users (subscribe [:users-in-open-group :online])]
    (fn []
      (let [users (->> @users
                       (remove (fn [user]
                                   (= @user-id
                                      (user :id)))))
            path (routes/users-page-path {:group-id @open-group-id})]
        [:div.users.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Users"}
                  (count users)]
                 [:div.modal
                  [:h2 "Online"]
                  (for [user users]
                    ^{:key (user :id)}
                    [user-pill-view (user :id)])]]))))

(defn tags-pane-view []
  (let [open-group-id (subscribe [:open-group-id])
        tags (subscribe [:tags])
        group-subscribed-tags (subscribe [:group-subscribed-tags])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "tags"})]
        [:div.tags.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Tags"}]
          [:div.modal
              (let [sorted-tags (->> @group-subscribed-tags
                                    (sort-by :threads-count)
                                     reverse)]
                (for [tag sorted-tags]
                  ^{:key (tag :id)}
                  [tag-pill-view (tag :id)]))]]))))

(defn current-user-button-view []
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url @user-id])
        open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "me"})]
        [:a {:href path
             :class (when (routes/current-path? path) "active")}
          [:img.avatar {:style {:background-color (id->color @user-id)}
                        :src @user-avatar-url}]]))))

(defn group-settings-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/group-settings-path {:group-id @group-id})]
        [:div.settings.shortcut {:class (when (routes/current-path? path) "active")}
         [:a.title {:href path
                    :title "Group Settings"}]]))))

(defn header-view []
  [:div.header
   [clear-inbox-button-view]
   [inbox-page-button-view]
   [recent-page-button-view]
   [users-online-pane-view]
   [tags-pane-view]
   [group-settings-view]
   [search-bar-view]
   [current-user-button-view]])
