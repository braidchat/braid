(ns braid.ui.views.header
  (:require [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]
            [chat.client.views.pills :refer [tag-view user-view]]
            [om.core :as om]))

(defn clear-inbox-button-view [{:keys [subscribe]}]
  (let [open-threads (subscribe [:open-threads])]
    (fn []
      [:div
        (when (< 5 (count @open-threads))
          [:button {:on-click (fn [_]
                                (dispatch! :clear-inbox))}
            "Clear Inbox"])])))

(defn inbox-page-button-view []
  (let [path (routes/inbox-page-path {:group-id (routes/current-group)})]
    [:div.inbox.shortcut {:className (when (routes/current-path? path) "active")}
      [:a {:href path
           :className "title"
           :title "Inbox"}]]))

(defn recent-page-button-view []
  (let [path (routes/recent-page-path {:group-id (routes/current-group)})]
    [:div.recent.shortcut {:className (when (routes/current-path? path) "active")}
      [:a {:href path
           :className "title"
           :title "Recent"}]]))

(defn help-page-pane-view []
  (let [path (routes/help-page-path {:group-id (routes/current-group)})]
    [:div.help.shortcut {:className (when (routes/current-path? path) "active")}
      [:a {:href path
           :className "title"
           :title "Help"}]
      [:div.modal
        [:p "Conversations must be tagged to be seen by other people."]
        [:p "Tag a conversation by mentioning a tag in a message: ex. #general"]
        [:p "You can also mention users to add them to a conversation: ex. @raf"]
        [:p "Add emoji by using :shortcodes: (they autocomplete)."]]]))

(defn users-online-pane-view [{:keys [subscribe]}]
  (let [user-id (subscribe [:user-id])
        users-in-open-group (subscribe [:users-in-open-group])
        users (->> @users-in-open-group
                   (remove (fn [user]
                                   (= @user-id
                                      (user :id)))))
        users-online (->> users
                          (filter (fn [user]
                                    (= :online
                                       (user :status)))))
        path (routes/users-page-path {:group-id (routes/current-group)})]
    (fn []
      [:div.users.shortcut {:className (when (routes/current-path? path) "active")}
        [:a {:href path
             :className "title"
             :title "Users"}
          (count users-online)]
       [:div.modal
        [:h2 "Online"]
        (for [user users-online]
          ;TODO: build online user divs (e.g. om/build user-view user)
          [:div "hello"])]])))



(defn header-view [props]
  [:div.header
    "Header"
    [clear-inbox-button-view props]
    [inbox-page-button-view]
    [recent-page-button-view]
    [help-page-pane-view]
    [users-online-pane-view props]])
