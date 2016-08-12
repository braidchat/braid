(ns braid.client.ui.views.call
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.webrtc :as rtc]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]]))

(defn ended-call-view
  [nickname]
  [:p (str "Call with " nickname " ended")])

(defn dropped-call-view
  [nickname]
  [:p (str "Call with " nickname " dropped")])

(defn declined-call-view
  [call]
  (let [current-user-id (subscribe [:user-id])
        caller-nickname (subscribe [:nickname (call :source-id)])
        callee-nickname (subscribe [:nickname (call :target-id)])]
    (fn [call]
      (if (is-caller? @current-user-id call)
        [:p (str @callee-nickname " declined your call")]
        [:p (str "Call with " @caller-nickname "declined")]))))

(defn accepted-call-view
  [call]
  (let [call-time (r/atom 0)
        current-user-id (subscribe [:user-id])
        caller-nickname (subscribe [:nickname (call :source-id)])
        callee-nickname (subscribe [:nickname (call :target-id)])]
    (fn [call]
      (js/setTimeout #(swap! call-time inc) 1000)
      [:div
       [:h4 (str "Call with " (if (is-caller? @current-user-id call) @callee-nickname @caller-nickname) "...")]
       [:div (str @call-time)]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       [:video {:class (if (= (call :type) :video) "video" "audio")}]
       [:a.button {:on-click
                    (fn [_]
                      (dispatch! :end-call (call :id)))} "End"]])))

(defn incoming-call-view
  [call]
  (let [current-user-id (subscribe [:user-id])]
    (fn [call]
      (if (= @current-user-id (call :target-id))

        [:div
           [:p (str "Call from " (call :source-id))]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :accept-call (call :id)))} "Accept"]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :decline-call (call :id)))} "Decline"]]

        [:div
           [:p (str "Calling " (call :target-id) "...")]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :drop-call (call :id)))} "Drop"]]))))

(defn during-call-view
  [call]
  (let [call-status (subscribe [:call-status (call :id)])
        current-user-id (subscribe [:user-id])
        caller-nickname (subscribe [:nickname (call :source-id)])
        callee-nickname (subscribe [:nickname (call :target-id)])
        nickname (if (is-caller? @current-user-id call) @callee-nickname @caller-nickname)]
    (fn [call]
      [:div
        (case @call-status
           :incoming [incoming-call-view call]
           :accepted [accepted-call-view call]
           :declined [declined-call-view call]
           :dropped [dropped-call-view nickname]
           :ended [ended-call-view nickname])])))

(defn before-call-view
  [callee-id]
  (let [caller-id (subscribe [:user-id])]
    (fn [callee-id]
      [:div.call
       [:div
        [:h3 "Call"]
        [user-pill-view callee-id]]
       [:br]
       [:a.button {:on-click
                    (fn [_]
                      (dispatch! :start-call {:type :audio
                                              :source-id caller-id
                                              :target-id callee-id}))}
        "Audio"]
       [:a.button {:on-click
                  (fn [_]
                    (dispatch! :start-call {:type :video
                                            :source-id caller-id
                                            :target-id callee-id}))}
        "Video"]])))

(defn call-view []
  (let [callee-id (subscribe [:page-id])
        new-call (subscribe [:new-call])]
    (fn []
      (if-not @new-call
        [before-call-view @callee-id]
        [during-call-view @new-call]))))
