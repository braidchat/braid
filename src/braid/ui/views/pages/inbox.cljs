(ns braid.ui.views.pages.inbox
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.thread :refer [thread-view]]
            [braid.ui.views.new-thread :refer [new-thread-view]]))

(defn inbox-page-view
  []
  (let [user-id (subscribe [:user-id])
        open-threads (subscribe [:open-threads])]
  (fn []
    (let [sorted-threads (->> @open-threads
                            ; sort by last message sent by logged-in user, most recent first
                           (sort-by
                                (comp (partial apply max)
                                      (partial map :created-at)
                                      (partial filter (fn [m] (= (m :user-id) @user-id)))
                                      :messages))
                              reverse)]
      [:div.page.inbox
        [:div.title "Inbox"]
        [:div.threads {:on-wheel ; make the mouse wheel scroll horizontally
                        (fn [e]
                          (let [target-classes (.. e -target -classList)
                                ; TODO: target threads by ref using reagent
                                this-elt (.. e -target)]
                            ; TODO: check if threads-div needs to scroll?
                            (when (and (or (.contains target-classes "thread")
                                           (.contains target-classes "threads"))
                                    (= 0 (.-deltaX e) (.-deltaZ e)))
                              (set! (.-scrollLeft this-elt)
                                    (- (.-scrollLeft this-elt) (.-deltaY e))))))}
          [new-thread-view]
          (for [thread sorted-threads]
            ^{:key (thread :id)}
            [thread-view thread])]]))))
