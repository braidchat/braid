(ns braid.ui.views.threads
  (:require [reagent.core :as r]
            [clojure.set :refer [difference]]
            [braid.ui.views.thread :refer [thread-view]]
            [braid.ui.views.new-thread :refer [new-thread-view]]))

; currently unused
(defn scroll-view
  []
  (let [this-elt (r/atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (reset! this-elt (r/dom-node this)))

       :reagent-render
       (fn []
         [:div.scroll {:on-wheel ; make the mouse wheel scroll horizontally
                       (fn [e]
                         (let [target-classes (.. e -target -classList)]
                           ; TODO: check if threads-div needs to scroll?
                           (when (and (or (.contains target-classes "thread")
                                          (.contains target-classes "threads"))
                                   (= 0 (.-deltaX e) (.-deltaZ e)))
                             (set! (.-scrollLeft @this-elt)
                                   (- (.-scrollLeft @this-elt) (.-deltaY e))))))}])})))

(defn threads-view
  [props]
  (let [threads (r/atom [])
        reset-threads! (fn [new-threads]
                         (println "reset!")
                         (reset! threads (vec new-threads)))
        update-threads! (fn [new-threads]
                          (swap! threads
                                 (fn [old-threads]
                                   (let [old-thread-ids (set (map :id old-threads))
                                         new-thread-ids (set (map :id new-threads))
                                         to-remove (difference old-thread-ids new-thread-ids)
                                         to-add (difference new-thread-ids old-thread-ids)]
                                     (vec (concat (remove (fn [t] (contains? to-remove (t :id))) old-threads)
                                                  (filter (fn [t] (contains? to-add (t :id))) new-threads)))))))]
    (r/create-class
      {:display-name "threads-view"

       :component-did-mount
       (fn [this]
         (reset-threads! (props :threads)))

       :component-will-receive-props
       (fn [this [_ next-props]]
         (if (not= ((r/props this) :group-id) (next-props :group-id))
           (reset-threads! (next-props :threads))
           (update-threads! (next-props :threads))))

       :reagent-render
       (fn [{:keys [new-thread-args
                    threads-opts]
             :or {threads-opts {}}}]
         [:div.threads threads-opts

          (when new-thread-args
            [new-thread-view new-thread-args])

          (doall
            (for [thread @threads]
              ^{:key (thread :id)}
              [thread-view thread]))])})))
