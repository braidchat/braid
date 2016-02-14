(ns chat.client.views.pages.recent
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]))

(defn recent-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page recent"}
        (dom/div #js {:className "title"} "Recent")
        (apply dom/div #js {:className "threads"}
          (map (fn [t] (om/build thread-view t {:key :id}))
               (let [group-id (data :open-group-id)
                     user-id (get-in @store/app-state [:session :user-id])]
                 ; sort by last message sent by logged-in user, most recent first
                 (->> (data :threads)
                      vals
                      (filter (fn [thread]
                                (or (empty? (thread :tag-ids))
                                    (contains?
                                      (into #{} (map store/group-for-tag) (thread :tag-ids))
                                      group-id))))
                      (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              (partial filter (fn [m] (= (m :user-id) user-id)))
                              :messages))
                      reverse))))))))
