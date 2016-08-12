(ns braid.client.ui.views.quests
  (:require [braid.client.state :refer [subscribe]]
            [braid.client.state.handler.quests :refer [quests]]
            [braid.client.dispatcher :refer [dispatch!]]))

(defn quest-view [quest]
  (let [completed? (subscribe [:quest-completed? (quest :id)])]
    (fn [quest]
      [:div.quest {:data-icon (quest :icon)}
       [:div
        [:h1 (quest :name)
             (if @completed?
               "(X)"
               "( )")]
        [:p (or (quest :description) "A short description would be here. Lorem ispum dolor it amet.")]]
       [:div.actions
        [:a.skip {:on-click (fn [_]
                              (dispatch! :skip-quest (quest :id)))} "Skip"]
        [:a.show-me {:href "#"} "Show Me"]]])))

(defn quests-menu-view []
  [:div.quests-menu
   [:div.content
    [:div.quests
     (for [quest (take 3 quests)]
       [quest-view quest])]]])

(defn quests-header-view []
  (let [completed-quest-count (subscribe [:completed-quest-count])]
    (fn []
      [:div.quests-header
       [:div.bar
        @completed-quest-count
        " "
        "Quests"]
       [quests-menu-view]])))

; TODO
; don’t have all of them displayed at the beginning
; notify when a task is completed
; notify when a new task added
; show overall progress
; show instructions when click show-me
; update descriptions
; update listeners
; consider requiring multiple of certain actions (and have progress)
