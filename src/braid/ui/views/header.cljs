(ns braid.ui.views.header
  (:require [chat.client.dispatcher :refer [dispatch!]]))

(defn clear-inbox-button-view [{:keys [subscribe]}]
  (let [open-threads (subscribe [:open-threads])]
    (fn []
      [:div
        (when (< 5 (count @open-threads))
          [:button {:on-click (fn [_]
                                (dispatch! :clear-inbox))}
            "Clear Inbox"])])))

(defn header-view [props]
  [:div.header
    "Header"
    [clear-inbox-button-view props]])