(ns chat.client.views.pages.inbox
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [braid.ui.views.thread :refer [ThreadView]]
            [chat.client.views.threads :refer [new-thread-view]]))

(defn inbox-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page inbox"}
        (dom/div #js {:className "title"} "Inbox")
        (apply dom/div #js {:className "threads"
                            :ref "threads-div"
                            :onWheel ; make the mouse wheel scroll horizontally
                            (fn [e]
                              (let [target-classes (.. e -target -classList)
                                    this-elt (om/get-node owner "threads-div")]
                                ; TODO: check if threads-div needs to scroll?
                                (when (and (or (.contains target-classes "thread")
                                               (.contains target-classes "threads"))
                                        (= 0 (.-deltaX e) (.-deltaZ e)))
                                  (set! (.-scrollLeft this-elt)
                                        (- (.-scrollLeft this-elt) (.-deltaY e))))))}
          (concat
            [(new-thread-view {})]
            (map (fn [t]
                   (ThreadView. #js {:thread t}))
                 (let [user-id (get-in @store/app-state [:session :user-id])]
                   ; sort by last message sent by logged-in user, most recent first
                   (-> data
                       store/open-threads
                       (->> (sort-by
                              (comp (partial apply max)
                                    (partial map :created-at)
                                    (partial filter (fn [m] (= (m :user-id) user-id)))
                                    :messages))
                            reverse))))))))))
