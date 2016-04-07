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

(defn header-view [props]
  [:div.header
    "Header"
    [clear-inbox-button-view props]
    [inbox-page-button-view]])
