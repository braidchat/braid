(ns braid.ui.views.pages.recent
  (:require [reagent.ratom :refer-macros [run!]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]
            [braid.ui.views.threads :refer [threads-view]]))

(defn recent-page-view
  []
  (let [group-id (subscribe [:open-group-id])
        threads (subscribe [:recent-threads] [group-id])
        user-id (subscribe [:user-id])
        load-recent (run! (dispatch! :load-recent-threads @group-id))]
    (fn []
      (let [_ @load-recent
            sorted-threads
            (->> @threads
                 ; sort by last message sent by logged-in user, most recent first
                 (sort-by
                   (comp (partial apply max)
                         (partial map :created-at)
                         (partial filter (fn [m] (= (m :user-id) @user-id)))
                         :messages))
                 reverse)]
        [:div.page.recent
          [:div.title "Recent"]
          [threads-view {:threads sorted-threads}]]))))
