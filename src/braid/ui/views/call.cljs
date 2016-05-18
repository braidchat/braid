(ns braid.ui.views.call
  (:require [reagent.core :as r]
            [reagent.impl.util :refer [extract-props]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.webrtc :as rtc]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]))

(defn call-interface-view
  [call]
  (let [call-time (r/atom 0)
        caller-id (call :source-id)
        user (subscribe [:user caller-id])]
    (fn []
      (js/setTimeout #(swap! call-time inc) 1000)
      [:div
       [:h4 "Call with " (@user :nickname) "..."]
       [:div (str @call-time)]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       (when (= (call :type) "video")
         [:video])
       [:a.button
        {:on-click
         (fn [_]
           (dispatch! :end-call call))}
        "End Call"]])))

(defn new-call-view
  [call]
  (let [user-id (subscribe [:user-id])]
    (fn [call]
      [:div
        (case (call :status)
           "incoming"
              (if (= @user-id (call :target-id))
                [:div
                   [:p (str "Call from" (call :source-id))]
                   [:a.button
                    {:on-click
                     (fn [_]
                       (dispatch! :accept-call call))}
                    "Accept"]
                   [:a.button
                    {:on-click
                     (fn [_]
                       (dispatch! :decline-call call))}
                    "Decline"]]
                [:div
                   [:p (str "Calling " (call :source-id) "...")]
                   [:a.button
                    {:on-click
                     (fn [_]
                       (dispatch! :drop-call call))}
                    "Drop"]])
           "accepted"
             [call-interface-view call]
           "declined"
             [:p "declined"]
           "ended"
             [:p "ended"]
           "dropped"
             [:p "dropped"])])))

(defn call-list-view
  []
  (let [calls (subscribe [:calls])]
    (fn []
      [:div
        (when (seq @calls)
          [:div.calls
            (doall
              (for [call @calls]
                ^{:key (call :id)}
                [new-call-view call]))])])))

(defn call-start-view
  [caller-id callee-id]
  (fn []
    [:div.call ;TODO: pass user to render pill
     [:div
      [:h3 "Call"]
      [user-pill-view callee-id]]
     [:br]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call (assoc {} :type "audio"
                                            :source-id caller-id
                                            :target-id callee-id)))}
      "Audio"]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call (assoc {} :type "video"
                                            :source-id caller-id
                                            :target-id callee-id))
           (dispatch! :request-ice-servers))}
      "Video"]
     [call-list-view]]))
