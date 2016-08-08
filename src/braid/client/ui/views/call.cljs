(ns braid.client.ui.views.call
  (:require [reagent.core :as r]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.webrtc :as rtc]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]]))

(defn dropped-call-view []
  [:p "dropped"])

(defn ended-call-view []
  [:p "ended"])

(defn declined-call-view []
  [:p "declined"])

(defn accepted-call-view
  [call]
  (let [call-time (r/atom 0)
        caller-id (call :source-id)
        user (subscribe [:user caller-id])]
    (fn [call]
      (js/setTimeout #(swap! call-time inc) 1000)
      [:div
       [:h4 "Call with " (@user :nickname) "..."]
       [:div (str @call-time)]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       (when (= (call :type) "video")
         [:video {:id "vid"}])
       [:a.button
        {:on-click
         (fn [_]
           (dispatch! :end-call call))}
        "End Call"]])))

(defn incoming-call-view [call]
  (let [user-id (subscribe [:user-id])]
    (fn [call]
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
            "Drop"]]))))

(defn call-view
  [call]
  (let [user-id (subscribe [:user-id])]
    (fn [call]
      [:div
        (case (call :status)
           :incoming [incoming-call-view]
           :accepted [accepted-call-view call]
           :declined [declined-call-view]
           :ended [ended-call-view]
           :dropped [dropped-call-view])])))

(defn pre-call-view
  [caller-id callee-id]
  (fn []
    [:div.call
     [:div
      [:h3 "Call"]
      [user-pill-view callee-id]]
     [:br]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call {:type :audio
                                   :source-id caller-id
                                   :target-id callee-id}))}
      "Audio"]
     [:a.button
       {:on-click
         (fn [_]
           (dispatch! :start-call {:type :video
                                   :source-id caller-id
                                   :target-id callee-id}))}
      "Video"]]))

(defn call-list-view
  []
  (let [calls (subscribe [:calls])]
    (fn []
      [:div.calls
        (doall
          (for [call @calls]
            ^{:key (call :id)}
            [call-view call]))])))
