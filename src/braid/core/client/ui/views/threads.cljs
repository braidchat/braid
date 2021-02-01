(ns braid.core.client.ui.views.threads
  (:require
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [clojure.set :refer [difference]]
   [reagent.core :as r]
   [reagent.dom :as r-dom]))

(defn threads-view
  [{:keys [new-thread-view group-id threads] :as props}]
  ;; for a better user experience, this component maintains order of threads
  ;; after mounting, any new threads that come in via props are appended to the end of existing threads
  ;; (except blank new threads, which are put at the front)

  ;; group-id prop is necessary to circumvent this 'thread caching' behaviour
  ;; when navigating to a different group's inbox
  (let [threads (r/atom [])
        this-elt (r/atom nil)
        reset-threads! (fn [threads']
                         (reset! threads (vec threads')))
        update-threads!
        (fn [target-threads]
          (swap! threads
                 (fn [existing-threads]
                   ;; 'existing threads' are the threads currently displayed (via previous props)
                   ;; 'target threads' are the threads we want displayed (via latest props)
                   ;; 'target threads' likely overlap with 'existing threads'
                   ;; 'blank threads' are target threads that have no messages
                   (let [blank-thread-ids (->> target-threads
                                               (filter (fn [thread]
                                                         (empty? (thread :messages))))
                                               (map :id)
                                               (set))
                         existing-thread-ids (set (map :id existing-threads))
                         target-thread-ids (difference (set (map :id target-threads))
                                                       blank-thread-ids)
                         to-remove (difference existing-thread-ids target-thread-ids)
                         to-add (difference target-thread-ids existing-thread-ids)
                         ;; here's the overall order:
                         ;;   blank threads (from target-threads)
                         ;;   existing threads (in their existing order) (minus any closed threads)
                         ;;   new threads (from target-threads)
                         ordered-ids (concat blank-thread-ids
                                             (remove to-remove existing-thread-ids)
                                             (filter to-add target-thread-ids))
                         target-threads-by-id (zipmap
                                                (map :id target-threads)
                                                target-threads)]
                     (mapv target-threads-by-id ordered-ids)))))
        scroll-horizontally
        (fn [e]
          (let [target-classes (.. e -target -classList)]
            ; TODO: check if threads-div needs to scroll?
            (when (and (or (.contains target-classes "thread")
                           (.contains target-classes "threads"))
                    (= 0 (.-deltaX e) (.-deltaZ e)))
              (set! (.-scrollLeft @this-elt)
                    (- (.-scrollLeft @this-elt) (.-deltaY e))))))]
    (r/create-class
      {:display-name "threads-view"

       :component-did-mount
       (fn [this]
         (reset-threads! (props :threads))
         (reset! this-elt (r-dom/dom-node this)))

       :component-will-receive-props
       (fn [this [_ next-props]]
         (if (not= ((r/props this) :group-id) (next-props :group-id))
           (reset-threads! (next-props :threads))
           (update-threads! (next-props :threads))))

       :reagent-render
       (fn [{:keys [new-thread-view]}]
         [:div.threads
          {:on-wheel scroll-horizontally}

          (when new-thread-view
            new-thread-view)

          (doall
            (for [thread @threads]
              ^{:key (thread :id)}
              [thread-view thread]))])})))
