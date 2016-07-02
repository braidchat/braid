(ns braid.client.ui.views.header
  (:require [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [->color]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.client.ui.views.search-bar :refer [search-bar-view]]))

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
          [:div.name (str "@" @user-nickname)]
          [:img.avatar {:src @user-avatar-url}]]))))

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

(defn invite-link-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/invite-page-path {:group-id @group-id})]
        [:a.invite-friend
         {:class (when (routes/current-path? path) "active")
          :href path}
         "Invite a Person"]))))

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

(defn group-bots-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/bots-path {:group-id @group-id})]
        [:a.group-bots
         {:class (when (routes/current-path? path) "active")
          :href path}
        "Bots"]))))

(defn group-uploads-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/uploads-path {:group-id @group-id})]
        [:a.group-uploads
         {:class (when (routes/current-path? path) "active")
          :href path}
         "Uploads"]))))

(defn left-header-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      [:div.left {:style {:background-color (->color @group-id)}}
       [group-name-view]
       [inbox-page-button-view]
       [recent-page-button-view]
       [search-bar-view]])))

(defn right-header-view []
  (let [user-id (subscribe [:user-id])]
    (fn []
      [:div.right
       [:div.bar {:style {:background-color (->color @user-id)}}
        [current-user-button-view]
        [:div.more]]
       [:div.options
        [subscriptions-link-view]
        [invite-link-view]
        [group-bots-view]
        [group-uploads-view]
        [edit-profile-link-view]
        [settings-link-view]]])))

(defn header-view []
  [:div.header
   [left-header-view]
   [right-header-view]])
