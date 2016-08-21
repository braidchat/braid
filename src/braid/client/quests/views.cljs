(ns braid.client.quests.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [braid.client.quests.list :refer [quests-by-id]]
            [braid.client.helpers :refer [->color]]))

(defn quest-view [quest-record]
  (let [show-video? (r/atom false)
        quest (quests-by-id (quest-record :quest-record/quest-id))]
    (fn [quest-record]
      [:div.quest
       [:div.main {:data-icon (quest :quest/icon)}
        [:div.info
         [:h1
          (quest :quest/name)
          (when (> (quest :quest/goal) 1)
            [:span.count " × " (quest :quest/goal)])]
         [:div.progress
          (for [i (range (quest :quest/goal))]
            ^{:key i}
            [:div.icon {:class (if (< i (quest-record :quest-record/progress))
                                 "complete"
                                 "incomplete")}])]

         [:p (quest :quest/description)]]

        (if (< (quest-record :quest-record/progress) (quest :quest/goal))
          [:div.actions
           [:a.skip {:on-click
                     (fn [_]
                       (dispatch [:quests/skip-quest (quest-record :quest-record/id)]))}
            "Skip"]
           (if @show-video?
             [:a.video.hide {:on-click (fn [_]
                                      (reset! show-video? false))}
              "Hide Vid"]
             [:a.video.show {:on-click (fn [_]
                                      (reset! show-video? true))}
              "Show Me"])]
          [:div.actions
           [:a.next
            {:on-click
             (fn [_]
               (dispatch [:quests/complete-quest (quest-record :quest-record/id)]))}
            "Get New Quest"]])]
       (when @show-video?
         [:div.video
          [:img {:src (quest :quest/video)}]])])))

(defn quests-menu-view []
  (let [active-quest-records (subscribe [:quests/active-quest-records])]
    (fn []
      [:div.quests-menu
       [:div.content
         (if (seq @active-quest-records)
           [:div.quests
            (for [quest-record @active-quest-records]
              ^{:key (quest-record :quest-record/id)}
              [quest-view quest-record])]

           [:div.congrats
            [:h1 "Congrats!"]
            [:p "You've completed all the quests we have so far. Stay tuned for more."]])]])))

(defn quests-header-view []
  (let [user-id (subscribe [:user-id])
        active-quest-records (subscribe [:quests/active-quest-records])]
    (fn []
      [:div.quests-header
       [:div.quests-icon {:style {:color (when (seq @active-quest-records)
                                           (->color @user-id))}}]
       [quests-menu-view]])))

