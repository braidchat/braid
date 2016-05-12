(ns braid.ui.views.call
  (:require [reagent.core :as r]
            [reagent.impl.util :refer [extract-props]]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn caller-tag-view
  [user-id]
  (fn []
    [:div
     [:h3 "Call"]
     [:a.button {:on-click
                 (fn [_]
                   (dispatch! :start-call
                              (assoc {} :id user-id :call-type "audio")))}
      "Audio"]
     [:a.button {:on-click
                 (fn [_]
                   (dispatch! :start-call
                              (assoc {} :id user-id :call-type "video")))}
      "Video"]]))

(defn calling-panel-view [])

(defn call-interface-view [])
