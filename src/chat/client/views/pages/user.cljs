(ns chat.client.views.pages.user
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.helpers :refer [user-cursor]]
            [chat.client.views.pills :refer [user-view]]))

(defn user-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [user-id (get-in @store/app-state [:page :id])
            user (om/observe owner (user-cursor user-id))]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            (om/build user-view user))
          (dom/div #js {:className "content"}
            (dom/div #js {:className "description"}
              (dom/img #js {:className "avatar" :src (user :avatar)})
              (dom/p nil "One day, a profile will be here.")
              (dom/p nil "Currently only showing your open threads that mention this user.")
              (dom/p nil "Soon, you will see all recent threads this user has participated in.")))
          (apply dom/div #js {:className "threads"
                              :ref "threads-div"
                              :onWheel (fn [e]
                                         (let [target-classes (.. e -target -classList)
                                               this-elt (om/get-node owner "threads-div")]
                                           ; TODO: check if threads-div needs to scroll?
                                           (when (and (or (.contains target-classes "thread")
                                                          (.contains target-classes "threads"))
                                                   (= 0 (.-deltaX e) (.-deltaZ e)))
                                             (set! (.-scrollLeft this-elt)
                                                   (- (.-scrollLeft this-elt) (.-deltaY e))))))}
            (concat
              [(new-thread-view {:mentioned-ids [user-id]})]
              (map (fn [t] (om/build thread-view t {:key :id}))
                   (let [current-user-id (get-in @store/app-state [:session :user-id])]
                     ; sort by last message sent by logged-in user, most recent first
                     (->> (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                          vals
                          (filter (fn [thread]
                                    (contains? (set (thread :mentioned-ids)) user-id)))
                          (sort-by
                            (comp (partial apply max)
                                  (partial map :created-at)
                                  (partial filter (fn [m] (= (m :user-id) current-user-id)))
                                  :messages))
                          reverse))))))))))
