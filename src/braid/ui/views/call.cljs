(ns braid.ui.views.call
  (:require [reagent.core :as r]
            [reagent.impl.util :refer [extract-props]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn call-start-view
  [callee-id]
  (fn []
    [:div.call ;TODO: pass user to render pill
     [:div
      [:h3 "Call"]
      [user-pill-view callee-id]]
     [:br]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call (assoc {} :callee-id callee-id
                                            :call-type "audio")))}
      "Audio"]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call (assoc {} :callee-id callee-id
                                            :call-type "video")))}
      "Video"]
     [call-list-view]]))

(defn call-list-view
  []
  (let [calls (subscribe [:calls])]
    (fn []
      (when (seq @calls)
        [:div
         [:h3 "Calling..."]
         [:div.calls
          (doall
            (for [call @calls]
              ^{:key (call :id)}
              [new-call-view call]))]]))))

(defn new-call-view
  [call]
  (fn []
    [:div
     [:p (str (call :id))]
     [:a.button
      {:on-click
       (fn [_] (dispatch! :accept-call (call :id)))}
      "Accept"]
     [:a.button
      {:on-click
       (fn [_] (dispatch! :decline-call (call :id)))}
      "Decline"]]))

(defn call-interface-view [])
