(ns braid.ui.views.header
  (:require [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.search-bar :refer [search-bar-view]]))

(defn inbox-page-button-view []
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/inbox-page-path {:group-id @open-group-id})]
        [:a.inbox {:class (when (routes/current-path? path) "active")
                   :href path
                   :title "Inbox"}]))))

(defn recent-page-button-view []
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/recent-page-path {:group-id @open-group-id})]
        [:a.recent {:class (when (routes/current-path? path) "active")
                    :href path
                    :title "Recent"}]))))

(defn current-user-button-view []
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url @user-id])
        user-nickname (subscribe [:nickname @user-id])
        open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "me"})]
        [:a.user-info {:href path
                       :class (when (routes/current-path? path) "active")}
          [:div.name @user-nickname]
          [:img.avatar {:style {:background-color (id->color @user-id)}
                        :src @user-avatar-url}]]))))

(defn group-name-view []
  (let [group (subscribe [:active-group])]
    (fn []
      [:div.group-name (@group :name)])))

(defn edit-profile-link-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/page-path {:group-id @group-id
                                    :page-id "me"})]
        [:a.edit-profile
         {:class (when (routes/current-path? path) "active")
          :href path}
         "Edit Your Profile"]))))

(defn subscriptions-link-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/page-path {:group-id @group-id
                                    :page-id "tags"})]
        [:a.subscriptions
         {:class (when (routes/current-path? path) "active")
          :href path}
         "Manage Subscriptions"]))))

(defn settings-link-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/group-settings-path {:group-id @group-id})]
        [:a.settings
         {:class (when (routes/current-path? path) "active")
          :href path}
         "Settings"]))))

(defn header-view []
  [:div.header

    [:div.left
      [group-name-view]
      [inbox-page-button-view]
      [recent-page-button-view]
      [search-bar-view]]

    [:div.right
      [:div.bar
        [current-user-button-view]
        [:div.more]]
      [:div.options
        [subscriptions-link-view]
        [:a.invite-friend {:href ""} "Invite a Friend"]
        [edit-profile-link-view]
        [settings-link-view]]]])
