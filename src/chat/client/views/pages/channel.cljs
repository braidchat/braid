(ns chat.client.views.pages.channel
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]))

(defn channel-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [tag-id (get-in @store/app-state [:page :id])
            tag (get-in @store/app-state [:tags tag-id])]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            "#" (tag :name))
          (apply dom/div #js {:className "threads"}
            (concat
              [(new-thread-view)]
              (map (fn [t] (om/build thread-view t {:key :id}))
                   (let [user-id (get-in @store/app-state [:session :user-id])]
                     ; sort by last reply
                     (->> (select-keys (data :threads) (get-in data [:page :search-result-ids]))
                          vals
                          (sort-by
                            (comp (partial apply max)
                                  (partial map :created-at)
                                  :messages))
                          reverse)
                     )))))))))
