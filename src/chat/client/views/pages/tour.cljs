(ns chat.client.views.pages.tour
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.store :as store]
            [chat.client.routes :as routes]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.shared.util :refer [if?]]))

(def ->seq (if? sequential? identity vector))

(defn element-offset
  [el]
  (loop [el el
         x 0 y 0]
    (if (and (some? el) (not (js/isNaN (.-offsetLeft el)))
          (not (js/isNaN (.-offsetTop el))))
      (recur (.-offsetParent el)
             (+ x (.-offsetLeft el)
                (- (.-scrollLeft el))
                (.-clientLeft el))
             (+ y (.-offsetTop el)
                (- (.-scrollTop el))
                (.-clientTop el)))
      {:top y :left x})))

(declare state->next state->prev)

(defn advance-state!
  [owner]
  (when-let [next-state (state->next (om/get-state owner :tour-state))]
    (om/set-state! owner :tour-state next-state)))

(defn retreat-state!
  [owner]
  (when-let [prev-state (state->prev (om/get-state owner :tour-state))]
    (om/set-state! owner :tour-state prev-state)))

(defn reset-tour!
  [owner]
  (om/set-state! owner :tour-state :initial)
  (om/set-state! owner :new-thread-id (uuid/make-random-squuid)))

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

(def tour-states
  "The states and corresponding view functions for each step in the tour. The
  states are specificied as a vector where the first value is a keyword
  indicating the name of the state, the second is a function which takes one
  argument (the container owner), and optionally a third argument which is a
  state transition function, which is called in will-update (useful if you want
  the state to change based on something happening, instead of just a button
  press)"
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
      (dom/div #js {:className "tour-msg left top arrow-left"}
        (dom/p nil "This sidebar shows the groups you are in")
        (dom/p nil "When you're in more than one group, you can click on "
          "the tiles here to switch between which one you're looking at")
        (prev-button owner)
        (next-button owner)))]

   [:inbox-button
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".inbox.shortcut")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is the inbox button")
          (prev-button owner)
          (next-button owner))))]

   [:recent-button
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".recent.shortcut")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is the recent button")
          (prev-button owner)
          (next-button owner))))]

   [:people-button
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".users.shortcut")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is the users button")
          (prev-button owner)
          (next-button owner))))]

   [:tags-button
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".tags.shortcut")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is the tags button")
          (prev-button owner)
          (next-button owner))))]

   [:history-search
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".search-bar")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is the history search field")
          (prev-button owner)
          (next-button owner))))]

   [:me-button
    (fn [owner]
      (let [inbox-btn (.querySelector js/document ".header .avatar")
            {:keys [top left]} (element-offset inbox-btn)]
        (dom/div #js {:className "tour-msg arrow-up"
                      :style #js {:top (str (+ top 30) "px")
                                  :left (str (- left 90) "px")}}
          (dom/p nil "This is will take you to your user profile page")
          (prev-button owner)
          (next-button owner))))]

   [:new-message
    (fn [owner]
      (let [thread-id (om/get-state owner :new-thread-id)]
        [(dom/div #js {:className "threads"}
           (new-thread-view {:id thread-id}))

         (dom/div #js {:className "tour-msg new-adjacent arrow-left"}
           (dom/p nil "Let's start a new thread that other people here will be able to see")
           (dom/p nil "Type some text (e.g. \"Hello Braid!\") in the adjacent box"
             " and hit return")
           (prev-button owner))]))
    (fn [owner next-props next-state]
      (when (contains? (next-props :threads) (next-state :new-thread-id))
        (advance-state! owner)))]

   [:mention-user
    (fn [owner]
      (let [thread-id (om/get-state owner :new-thread-id)
            thread (get-in (om/get-props owner) [:threads thread-id])]
        [(dom/div #js {:className "threads"}
           (om/build thread-view thread))
         (dom/div #js {:className "tour-msg new-adjacent arrow-left"}
           (dom/p nil "Now let's mention a user")
           (dom/p nil "Start typing \"@\" and select a user you want to mention,"
            " then hit enter")
           (dom/p nil "(maybe the person who invited you!)"))]))
    (fn [owner next-props next-state]
      (let [thread (get-in next-props [:threads (next-state :new-thread-id)])]
        (when-not (empty? (thread :mentioned-ids))
          (advance-state! owner))))]

   [:tag-thread
    (fn [owner]
      (let [thread-id (om/get-state owner :new-thread-id)
            thread (get-in (om/get-props owner) [:threads thread-id])]
        [(dom/div #js {:className "threads"}
           (om/build thread-view thread))
         (dom/div #js {:className "tour-msg new-adjacent"}
           (dom/p nil "Let's make this thread we've started publicly viewable to the group")
           (dom/p nil "We do this by adding a tag:")
           (dom/p nil "Starting type \"#\", find a tag in the autocomplete, and hit enter")
           (dom/p nil "(something like \"general\" or \"watercooler\" is "
             " probably a good place for this)")
           )]))
    (fn [owner next-props next-state]
      (let [thread (get-in next-props [:threads (next-state :new-thread-id)])]
        (when-not (empty? (thread :tag-ids))
          (advance-state! owner))))]

   [:end
    (fn [owner]
      (let [thread-id (om/get-state owner :new-thread-id)
            thread (get-in (om/get-props owner) [:threads thread-id])]
        [(dom/div #js {:className "threads"}
           (om/build thread-view thread))
         (dom/div #js {:className "tour-msg center"}
           (dom/h1 nil "Ready to Go!")
           (dom/p nil "Now you are ready to start using Braid in earnest")
           (dom/p nil "Click "
             (dom/a #js {:href (routes/inbox-page-path {:group-id (routes/current-group)})}
               "here")
             " to go to your inbox and start chatting in earnest!")
           (dom/button #js {:onClick (fn [_] (reset-tour! owner))}
             "Restart the tour"))]))]
   ])

(def state->next
  "Map of current-state to next-state. (e.g. {:initial :sidebar, :sidebar :end})"
  (->> tour-states (map first) (partition 2 1) (into {} (map vec))))
(def state->prev
  "Inverse of state->next, to go from current state to previous state"
  (into {} (map (fn [[a b]] [b a])) state->next))

(def state->view
  "Map of state to view function"
  (into {} (map (fn [[st v]] [st v])) tour-states))

(def state->update-fn
  "Map of state name to update-fn (to be called in will-update)"
  (into {} (comp (remove (fn [[_ up]] (nil? up)))
                 (map (fn [[st _ up]] [st up])))
        tour-states))

(defn tour-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:tour-state :initial
       :new-thread-id (uuid/make-random-squuid)})
    om/IWillUpdate
    (will-update [_ next-props next-state]
      (when-let [f (state->update-fn (next-state :tour-state))]
        (f owner next-props next-state)))
    om/IRenderState
    (render-state [_ {:keys [tour-state] :as state}]
      (apply dom/div #js {:className "page tour"}
        (dom/div #js {:className "title"} "Braid Tour")
        (->seq ((state->view tour-state) owner))))))
