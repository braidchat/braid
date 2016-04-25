(ns braid.ui.views.pages.inbox
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.threads :refer [threads-view]]))

(defn inbox-page-view
  []
  (let [user-id (subscribe [:user-id])
        open-threads (subscribe [:open-threads])]
    (fn []
      (let [sorted-threads
            (->> @open-threads
                 ; sort by last message sent by logged-in user, most recent first
                 (sort-by
                   (comp (partial apply max)
                         (partial map :created-at)
                         (partial filter (fn [m] (= (m :user-id) @user-id)))
                         :messages))
                 reverse)]

        [:div.page.inbox
         [:div.title "Inbox"]
         [threads-view {:new-thread-args {}
                        :threads sorted-threads}]]))))
