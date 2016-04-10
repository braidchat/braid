(ns braid.ui.views.header
  (:require [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [id->color]]
            [braid.ui.views.pills :refer [tag-pill-view user-pill-view]]
            [braid.ui.views.search-bar :refer [search-bar-view]]))

(defn clear-inbox-button-view [{:keys [subscribe]}]
  (let [open-threads (subscribe [:open-threads])]
    (fn []
      [:div
        (when (< 5 (count @open-threads))
          [:button {:on-click (fn [_]
                                (dispatch! :clear-inbox))}
            "Clear Inbox"])])))

(defn inbox-page-button-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/inbox-page-path {:group-id @open-group-id})]
        [:div.inbox.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Inbox"}]]))))

(defn recent-page-button-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/recent-page-path {:group-id @open-group-id})]
        [:div.recent.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Recent"}]]))))

(defn help-page-pane-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/help-page-path {:group-id @open-group-id})]
        [:div.help.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Help"}]
          [:div.modal
            [:p "Conversations must be tagged to be seen by other people."]
            [:p "Tag a conversation by mentioning a tag in a message: ex. #general"]
            [:p "You can also mention users to add them to a conversation: ex. @raf"]
            [:p "Add emoji by using :shortcodes: (they autocomplete)."]]]))))

(defn users-online-pane-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])
        user-id (subscribe [:user-id])
        users-in-open-group (subscribe [:users-in-open-group])]
    (fn []
      (let [users (->> @users-in-open-group
                   (remove (fn [user]
                                   (= @user-id
                                      (user :id)))))
            users-online (->> users
                              (filter (fn [user]
                                        (= :online
                                           (user :status)))))
            path (routes/users-page-path {:group-id @open-group-id})]
        [:div.users.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Users"}
                  (count users-online)]
                 [:div.modal
                  [:h2 "Online"]
                  (for [user users-online]
                    [user-pill-view user subscribe])]]))))


(defn tags-pane-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])
        tags (subscribe [:tags])
        group-subscribed-tags (subscribe [:group-subscribed-tags])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "channels"})]
        [:div.tags.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Tags"}]
          [:div.modal
              (let [sorted-tags (->> @group-subscribed-tags
                                    (sort-by :threads-count)
                                     reverse)]
                (for [tag sorted-tags]
                  [tag-pill-view tag subscribe]))]]))))

(defn current-user-button-view [{:keys [subscribe]}]
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url])
        open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/page-path {:group-id @open-group-id
                                    :page-id "me"})]
        [:a {:href path
             :class (when (routes/current-path? path) "active")}
          [:img.avatar {:style {:background-color (id->color @user-id)}
                        :src @user-avatar-url}]]))))

(defn extensions-page-button-view [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])]
    (fn []
      (let [path (routes/extensions-page-path {:group-id @open-group-id})]
        [:div.extensions.shortcut {:class (when (routes/current-path? path) "active")}
          [:a.title {:href path
                     :title "Extensions"}]
                [:div.modal
                  [:div "extensions"]]]))))

(defn header-view [props]
  [:div.header
    [clear-inbox-button-view props]
    [inbox-page-button-view props]
    [recent-page-button-view props]
    [help-page-pane-view props]
    [users-online-pane-view props]
    [tags-pane-view props]
    [search-bar-view props]
    [current-user-button-view props]
    #_[extensions-page-button-view]]) ;Extensions page button temporarily not rendered
