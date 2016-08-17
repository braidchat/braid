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
        [:a.button {:on-click (fn [_] (dispatch! :set-requester-call-status [call :archived]))} "X"]
        [:p (str "Call with " @correct-nickname " ended")]])))

(defn dropped-call-view
  [call]
  (let [correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch! :set-requester-call-status [call :archived]))} "X"]
        [:p (str "Call with " @correct-nickname " dropped")]])))

(defn declined-call-view
  [call]
  (let [user-is-caller? (subscribe [:current-user-is-caller? (call :caller-id)])
        caller-nickname (subscribe [:nickname (call :caller-id)])
        callee-nickname (subscribe [:nickname (call :callee-id)])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch! :set-requester-call-status [call :archived]))} "X"]
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
       [:video {:id "video"
                :class (if (= (call :type) :video) "video" "audio")}]
       [:a.button {:on-click
                    (fn [_]
                      (dispatch! :set-requester-call-status [call :ended]))} "End"]])))

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
                          (dispatch! :set-requester-call-status [call :dropped]))} "Drop"]]
        [:div
           [:p (str "Call from " @caller-nickname)]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :set-requester-call-status [call :accepted]))} "Accept"]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :set-requester-call-status [call :declined]))} "Decline"]]))))

(defn during-call-view
  [call]
  (let [call-atom (r/atom call)
        call-status (subscribe [:call-status] [call-atom])
        correct-nickname (subscribe [:correct-nickname] [call-atom])]
    (r/create-class
      {:display-name "during-call-view"
       :component-will-receive-props
       (fn [_ [_ new-call]]
         (reset! call-atom new-call))
       :reagent-render
       (fn [call]
         [:div
           (case @call-status
             :incoming [incoming-call-view call]
             :accepted [accepted-call-view call]
             :declined [declined-call-view call]
             :dropped [dropped-call-view call]
             :ended [ended-call-view call])])})))

(defn before-call-view
  [callee-id]
  (let [callee-id-atom (r/atom callee-id)
        caller-id (subscribe [:user-id])
        callee-nickname (subscribe [:nickname] [callee-id-atom])]
    (r/create-class
      {:display-name "before-call-view"
       :component-will-receive-props
       (fn [_ [_ new-callee-id]]
         (reset! callee-id-atom new-callee-id))
       :reagent-render
       (fn [callee-id]
         [:div.call
           [:h3 (str "Call " @callee-nickname)]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :start-new-call {:type :audio
                                                      :caller-id @caller-id
                                                      :callee-id callee-id}))} "Audio"]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch! :start-new-call {:type :video
                                                      :caller-id @caller-id
                                                      :callee-id callee-id}))} "Video"]])})))

(defn call-view []
  (let [callee-id (subscribe [:page-id])
        new-call (subscribe [:new-call])]
    (fn []
      (if @new-call
        [during-call-view @new-call]
        [before-call-view @callee-id]))))
