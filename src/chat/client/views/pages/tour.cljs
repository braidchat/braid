(ns chat.client.views.pages.tour
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.routes :as routes]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.shared.util :refer [if?]]))

(def ->seq (if? sequential? identity vector))

(declare state->next state->prev)

(defn advance-state!
  [owner]
  (when-let [next-state (state->next (om/get-state owner :tour-state))]
    (om/set-state! owner :tour-state next-state)))

(defn retreat-state!
  [owner]
  (when-let [prev-state (state->prev (om/get-state owner :tour-state))]
    (om/set-state! owner :tour-state prev-state)))

(defn next-button
  [owner]
  (dom/button #js {:onClick (fn [_] (advance-state! owner))
                   :className "forward"}
    "Next"))

(defn prev-button
  [owner]
  (dom/button #js {:onClick (fn [_] (retreat-state! owner))
                   :className "back"}
    "Back"))

; tutorial view. show:
; - groups on the side:
;  - show how clicking on group selects which one you're in
; - new message box:
;  - explain how tagging works
;   - tag
;   - user mention
;  - explain other special things:
;    - links, emphasis, images, file upload
; - existing threads:
;  - types of threads:
;   - threads with no tags
;   - "private" threads
;   - threads with tags
;  - explain how threads can be closed & get re-opened?
; - top bar:
;  - inbox
;  - recent
;  - users
;  - tags
;  - help
;  - search
;  - me/settings
; - other pages
;  - user page
;  - tag/channel page

(def tour-states
  "The states and corresponding view functions for each step in the tour"
  [
   [:initial
    (fn [owner]
      (dom/div #js {:className "tour-msg center"}
        (dom/h1 nil "Welcome to Braid!")
        (dom/p nil "Braid is a little different from other chat programs "
          "you might be used to")
        (dom/p nil "Let's have a quick tour of how this works")
        (next-button owner)))]

   [:sidebar
    (fn [owner]
      (dom/div #js {:className "tour-msg left top arrow arrow-left"}
        (dom/p nil "This sidebar shows the groups you are in")
        (dom/p nil "When you're in more than one group, you can click on "
          "the tiles here to switch between which one you're looking at")
        (prev-button owner)
        (next-button owner)))]

   [:new-message
    (fn [owner]
      [(dom/div #js {:className "threads"}
         (new-thread-view {}))

       (dom/div #js {:className "tour-msg new-adjacent"}
         (dom/p nil "Let's start a new thread")
         (dom/p nil "Type some text (e.g. \"Hello Braid!\") in the adjacent box"
           " and hit return")
         (prev-button owner))])]

   [:end
    (fn [owner]
      (dom/div #js {:className "tour-msg center"}
        (dom/h1 nil "Ready to Go!")
        (dom/p nil "Now you are ready to start using Braid in earnest")
        (dom/p nil "Click "
          (dom/a #js {:href (routes/inbox-page-path {:group-id (routes/current-group)})}
            "here")
          " to go to your inbox and start chatting in earnest!")
        (prev-button owner)
        ))]
   ])

(def state->next
  "Map of current-state to next-state. (e.g. {:initial :sidebar, :sidebar :end})"
  (->> tour-states (map first) (partition 2 1) (into {} (map vec))))
(def state->prev
  "Inverse of state->next, to go from current state to previous state"
  (into {} (map (fn [[a b]] [b a])) state->next))

(def state->view
  "Map of state to view function"
  (into {} tour-states))

(defn tour-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:tour-state :initial})
    om/IRenderState
    (render-state [_ {:keys [tour-state] :as state}]
      (apply dom/div #js {:className "page tour"}
        (dom/div #js {:className "title"} "Braid Tour")
        (->seq ((state->view tour-state) owner))))))
