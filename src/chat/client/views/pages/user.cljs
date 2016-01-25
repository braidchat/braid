(ns chat.client.views.pages.user
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.pills :refer [user-view]]))

(defn user-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [user-id (get-in @store/app-state [:page :id])
            user (get-in @store/app-state [:users user-id])]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            (om/build user-view user))
          (dom/div #js {:className "description"}
            (dom/img #js {:className "avatar" :src (user :avatar)})
            (dom/span nil "One day, a profile will be here."))
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
