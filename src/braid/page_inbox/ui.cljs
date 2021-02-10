(ns braid.page-inbox.ui
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.core.client.ui.views.threads :refer [threads-view]]
    [braid.lib.color :as color]))

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
                      (dispatch [:clear-inbox!]))}
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
     ^{:key group-id} ;; necessary to prevent component-reuse when switching groups
     [threads-view {:new-thread-view [new-thread-view {:group-id group-id}]
                    :threads open-threads}]]))
