(ns braid.client.quests.views
  (:require [reagent.core :as r]
            [braid.client.state :refer [subscribe]]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.helpers :refer [->color]]))

(defn quest-view [quest]
  (let [show-video? (r/atom false)]
    (fn [quest]
      [:div.quest
       [:div.main {:data-icon (quest :icon)}
        [:div.info
         [:h1 (quest :name)]
         [:div.progress
          (for [i (range (quest :goal))]
            [:div.icon {:class (if (< i (quest :progress))
                                 "complete"
                                 "incomplete")}])]

         [:p (or (quest :description) "A short description would be here. Lorem ispum dolor it amet.")]]

        (if (< (quest :progress) (quest :goal))
          [:div.actions
           [:a.skip {:on-click (fn [_]
                                 (dispatch! :quests/skip-quest (quest :id)))} "Skip"]
           (if @show-video?
             [:a.video.hide {:on-click (fn [_]
                                      (reset! show-video? false))}
              "Hide Vid"]
             [:a.video.show {:on-click (fn [_]
                                      (reset! show-video? true))}
              "Show Me"])]
          [:div.actions
           [:a.next {:on-click (fn [_]
                                 (dispatch! :quests/get-next-quest (quest :id)))}
            "Get New Quest"]])]
       (when @show-video?
         [:div.video
          [:img {:src (quest :video)}]])])))

(defn quests-menu-view []
  (let [active-quests (subscribe [:quests/active-quests])]
    (fn []
      [:div.quests-menu
       [:div.content
         (if (seq @active-quests)
           [:div.quests
            (for [quest @active-quests]
              ^{:key (quest :id)}
              [quest-view quest])]

           [:div.congrats
            [:h1 "Congrats!"]
            [:p "You've completed all the quests we have so far. Stay tuned for more."]])]])))

(defn quests-header-view []
  (let [user-id (subscribe [:user-id])]
    (fn []
      [:div.quests-header
       [:div.quests-icon {:style {:color (->color @user-id)}}]
       [quests-menu-view]])))

