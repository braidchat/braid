(ns braid.client.ui.views.call
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.webrtc :as rtc]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]]))

(defn ended-call-view
  [call]
  (let [correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch! :archive-call call))} "X"]
        [:p (str "Call with " @correct-nickname " ended")]])))

(defn dropped-call-view
  [call]
  (let [correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch! :archive-call call))} "X"]
        [:p (str "Call with " @correct-nickname " dropped")]])))

(defn declined-call-view
  [call]
  (let [user-is-caller? (subscribe [:current-user-is-caller? (call :caller-id)])
        caller-nickname (subscribe [:nickname (call :caller-id)])
        callee-nickname (subscribe [:nickname (call :callee-id)])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch! :archive-call call))} "X"]
        (if @user-is-caller?
          [:p (str @callee-nickname " declined your call")]
          [:p (str "Call with " @caller-nickname "declined")])])))

(defn accepted-call-view
  [call]
  (let [call-time (r/atom 0)
        correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      (js/setTimeout #(swap! call-time inc) 1000)
      [:div
       [:h4 (str "Call with " @correct-nickname "...")]
       [:div (str @call-time)]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       [:video {:class (if (= (call :type) :video) "video" "audio")}]
       [:a.button {:on-click
                    (fn [_]
                      (dispatch! :end-call call))} "End"]])))

(defn incoming-call-view
  [call]
  (let [user-is-caller? (subscribe [:current-user-is-caller? (call :caller-id)])
        caller-nickname (subscribe [:nickname (call :caller-id)])
        callee-nickname (subscribe [:nickname (call :callee-id)])]
    (fn [call]
      (if @user-is-caller?
        [:div
           [:p (str "Calling " @callee-nickname "...")]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :drop-call call))} "Drop"]]
        [:div
           [:p (str "Call from " @caller-nickname)]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :accept-call call))} "Accept"]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :decline-call call))} "Decline"]]))))

(defn during-call-view
  [call]
  (let [call-status (subscribe [:call-status (call :id)])
        correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        (case @call-status
           :incoming [incoming-call-view call]
           :accepted [accepted-call-view call]
           :declined [declined-call-view (call :caller-id) (call :caller-id)]
           :dropped [dropped-call-view @correct-nickname]
           :ended [ended-call-view @correct-nickname])])))

(defn before-call-view
  [callee-id]
  (let [caller-id (subscribe [:user-id])
        callee-nickname (subscribe [:nickname callee-id])]
    (fn [callee-id]
      [:div.call
      [:h3 (str "Call " @callee-nickname)]
      [:a.button {:on-click
                   (fn [_]
                     (dispatch! :start-call {:type :audio
                                             :caller-id @caller-id
                                             :callee-id callee-id}))} "Audio"]
      [:a.button {:on-click
                   (fn [_]
                     (dispatch! :start-call {:type :video
                                             :caller-id @caller-id
                                             :callee-id callee-id}))} "Video"]])))

(defn call-view []
  (let [callee-id (subscribe [:page-id])
        new-call (subscribe [:new-call])]
    (fn []
      (if @new-call
        [during-call-view @new-call]
        [before-call-view @callee-id]))))
