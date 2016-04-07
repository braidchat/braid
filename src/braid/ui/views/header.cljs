(ns braid.ui.views.header
  (:require [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]))

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
(defn header-view [props]
  [:div.header
    "Header"
    [clear-inbox-button-view props]
    [inbox-page-button-view]
    [recent-page-button-view]])
