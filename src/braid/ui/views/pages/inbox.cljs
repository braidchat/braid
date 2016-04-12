(ns braid.ui.views.pages.inbox
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view new-thread-view]]))

(defn inbox-page-view
  [data subscribe]
  (fn []
    (let [user-id (subscribe [:user-id])
          open-threads (subscribe [:open-threads])]
      [:div.page.inbox
      [:div.title "Inbox"]
      [:div.threads {:ref "threads-div"
                     :on-wheel ; make the mouse wheel scroll horizontally
                      (fn [e]
                        (let [target-classes (.. e -target -classList)
                              this-elt (.. e -target)]
                          ; TODO: check if threads-div needs to scroll?
                          (when (and (or (.contains target-classes "thread")
                                         (.contains target-classes "threads"))
                                  (= 0 (.-deltaX e) (.-deltaZ e)))
                            (set! (.-scrollLeft this-elt)
                                  (- (.-scrollLeft this-elt) (.-deltaY e))))))}
        [(new-thread-view {})]
        (for [thread open-threads]
          (om/build thread-view thread {:key :id}))
             ; sort by last message sent by logged-in user, most recent first
             (-> data
                 @open-threads
                 (->> (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              (partial filter (fn [m] (= (m :user-id) @user-id)))
                              :messages))
                      reverse))]])))