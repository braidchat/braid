(ns braid.ui.views.pages.recent
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]))

(defn recent-page-view [data subscribe]
  (let [group-id (data :open-group-id)
        threads (data :threads)]
    (fn []
      [:div.page.recent
        [:div.title "Recent"]
        [:div.threads
          (for [thread threads]
            (om/build thread-view thread {:key :id}))
               (let [user-id (subscribe [:user-id])
                     group-for-tag (subscribe [:group-for-tag])]
                 ; sort by last message sent by logged-in user, most recent first
                 (->> threads
                      vals
                      (filter (fn [thread]
                                (or (empty? (thread :tag-ids))
                                    (contains?
                                      (into #{} (map group-for-tag) (thread :tag-ids))
                                      group-id))))
                      (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              (partial filter (fn [m] (= (m :user-id) @user-id)))
                              :messages))
                      reverse))]])))