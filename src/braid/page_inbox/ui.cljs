(ns braid.page-inbox.ui
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [braid.core.client.ui.views.threads :refer [threads-view]]
    [braid.lib.color :as color]))

(defn new-thread-view
  [{:keys [on-click group-id]}]
  [:button.new-thread
   {:title "Start New Thread"
    :style {:background-color (color/->color group-id)}
    :on-click on-click}
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

(defn inbox-view
  [{:keys [inbox-id open-threads new-thread-on-click group-id]}]
  (r/with-let [resort-nonce (r/atom 0)
               thread-order-dirty? (r/atom false)]
    [:div.inbox
     [threads-view {:new-thread-view [:div.sidebar
                                      (when @thread-order-dirty?
                                        [:button.resort-inbox
                                         {:on-click (fn [_]
                                                      (swap! resort-nonce inc))}
                                         "Resort Inbox"])
                                      [new-thread-view {:on-click new-thread-on-click
                                                        :group-id group-id}]]
                    :threads open-threads
                    :resort-nonce [inbox-id @resort-nonce]
                    :thread-order-dirty? thread-order-dirty?}]]))

(defn inbox-page-view
  []
  (let [group-id @(subscribe [:open-group-id])
        group @(subscribe [:active-group])
        open-threads @(subscribe [:open-threads group-id])
        new-thread-on-click (fn []
                              (dispatch [:create-thread! {:group-id group-id}]))]
    [:div.page.inbox
     [:div.intro
      (:intro group)
      [clear-inbox-button-view]]
     [inbox-view {:inbox-id group-id
                  :open-threads open-threads
                  :new-thread-on-click new-thread-on-click
                  :group-id group-id}]]))
