(ns braid.ui.views.threads
  (:require [reagent.core :as r]
            [braid.ui.views.thread :refer [thread-view]]
            [braid.ui.views.new-thread :refer [new-thread-view]]))

(defn threads-view
  [new-thread-args threads]
  (let [this-elt (r/atom nil)]
    (r/create-class
      {:display-name "threads-view"
       :component-did-mount
       (fn [this]
         (reset! this-elt (r/dom-node this)))

       :reagent-render
       (fn [new-thread-args threads]
         [:div.threads
          {:on-wheel ; make the mouse wheel scroll horizontally
           (fn [e]
             (let [target-classes (.. e -target -classList)]
               ; TODO: check if threads-div needs to scroll?
               (when (and (or (.contains target-classes "thread")
                              (.contains target-classes "threads"))
                       (= 0 (.-deltaX e) (.-deltaZ e)))
                 (set! (.-scrollLeft @this-elt)
                       (- (.-scrollLeft @this-elt) (.-deltaY e))))))}

          (when new-thread-args
            [new-thread-view new-thread-args])

          (doall
            (for [thread threads]
              ^{:key (thread :id)}
              [thread-view thread]))])})))
