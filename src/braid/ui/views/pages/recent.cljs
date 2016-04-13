(ns braid.ui.views.pages.recent
  (:require [braid.ui.views.thread :refer [thread-view]]))

(defn recent-page-view-test
  [{:keys [subscribe]}]
  (let [group-id (subscribe [:open-group-id])
        threads (subscribe [:get-threads-for-group @group-id])
        user-id (subscribe [:user-id])]
    (fn []
      (let [sorted-threads (->> @threads
                            ; sort by last message sent by logged-in user, most recent first
                            (sort-by
                              (comp (partial apply max)
                                    (partial map :created-at)
                                    (partial filter (fn [m] (= (m :user-id) @user-id)))
                                    :messages))
                             reverse)]
        [:div.page.recent
          [:div.title "Recent"]
          [:div.threads
            (for [thread sorted-threads]
              ^{:key (thread :id)}
              [thread-view thread])]]))))
