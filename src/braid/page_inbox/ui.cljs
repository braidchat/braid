(ns braid.page-inbox.ui
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [braid.core.client.ui.styles.mixins :as mixins]
    [braid.core.client.ui.views.threads :refer [threads-view]]
    [braid.lib.color :as color]))

(def styles
  [:>.page.inbox

   [:>.threads

    [:>.new-thread
     {;; background is set inline to group-color
      :border-radius "50%"
      :border "none"
      :flex-shrink 0
      :color "white"
      :font-size "4em"
      :width "5rem"
      :height "5rem"
      :margin "0 2rem 2rem 1rem"
      :cursor "pointer"}

     [:&:hover
      ;; double invert *trick* keeps the + white
      {:filter "invert(100%) brightness(1.8) invert(100%)"}]]]

   [:>.intro

    [:>button.clear-inbox
     (mixins/outline-button
       {:text-color "#aaa"
        :border-color "#ccc"
        :hover-text-color "#999"
        :hover-border-color "aaa"
        :icon \uf0d0})]]])

(defn new-thread-view [{:keys [group-id] :as  thread-opts}]
  [:button.new-thread
   {:title "Start New Thread"
    :style {:background-color (color/->color group-id)}
    :on-click
    (fn []
      (dispatch [:create-thread! thread-opts]))}
   "+"])

(defn clear-inbox-button-view []
  (let [group-id (subscribe [:open-group-id])
        open-threads (subscribe [:open-threads] [group-id])]
    (fn []
      (when (< 5 (count @open-threads))
        [:button.clear-inbox
         {:on-click (fn [_]
                      (dispatch [:clear-inbox]))}
         "Clear Inbox"]))))

(defn inbox-page-view
  []
  (let [group-id @(subscribe [:open-group-id])
        group @(subscribe [:active-group])
        open-threads @(subscribe [:open-threads group-id])]
    [:div.page.inbox
     [:div.intro
      (:intro group)
      [clear-inbox-button-view]]
     [threads-view {:new-thread-view [new-thread-view {:group-id group-id}]
                    :group-id group-id
                    :threads open-threads}]]))
