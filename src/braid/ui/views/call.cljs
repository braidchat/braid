(ns braid.ui.views.call
  (:require [reagent.core :as r]
            [reagent.impl.util :refer [extract-props]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]))



(defn call-interface-view
  [call]
  (let [caller-id (call :source-id)
        user (subscribe [:user caller-id])]
    (fn []
      [:div
       [:h4 "Call with " (@user :nickname) "..."]
       [:div "seconds"]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       (when (= (call :type) "video")
         [:video])
       [:a.button
        {:on-click
         (fn [_]
           (dispatch! :end-call (call :id)))} "End Call"]])))


(defn new-call-view
  [call]
  [:div
    (case (call :status)
      "incoming"
        [:div
         [:p (str (call :id))]
         [:a.button
          {:on-click
           (fn [_]
             (dispatch! :accept-call (call :id)))}
          "Accept"]
         [:a.button
          {:on-click
           (fn [_]
             (dispatch! :decline-call (call :id)))}
          "Decline"]]
      "accepted"
        [call-interface-view call]
      "declined"
        [:p "declined"]
      "ended"
        [:p "ended"])])

(defn call-list-view
  []
  (let [calls (subscribe [:calls])]
    (fn []
      (when (seq @calls)
        [:div
         [:div.calls
          (doall
            (for [call @calls]
              ^{:key (call :id)}
              [new-call-view call]))]]))))

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
