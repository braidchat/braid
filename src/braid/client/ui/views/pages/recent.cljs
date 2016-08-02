(ns braid.client.ui.views.pages.recent
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [run!]]
            [braid.client.state :refer [subscribe]]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.ui.views.threads :refer [threads-view]]))

(defn recent-page-view
  []
  (let [group-id (subscribe [:open-group-id])
        threads (subscribe [:recent-threads] [group-id])
        user-id (subscribe [:user-id])
        page (subscribe [:page])]
    (fn []
      (let [sorted-threads
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
         (if (and (not (@page :loading?)) (empty? sorted-threads))
           [:p "No recent threads in this group"]
           [threads-view {:threads sorted-threads}])]))))

