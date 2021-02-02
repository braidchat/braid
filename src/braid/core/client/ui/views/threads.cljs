(ns braid.core.client.ui.views.threads
  (:require
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [clojure.set :refer [difference]]
   [reagent.core :as r]
   [reagent.dom :as r-dom]))

(defn threads-view
  [{:keys [new-thread-view threads] :as props}]
  ;; to avoid accidental re-use when switching groups, key this component with the group-id

  ;; for a better user experience, this component maintains order of threads
  ;; after mounting, any new threads that come in via props are appended to the end of existing threads
  ;; (except blank new threads, which are put at the front)
  (let [threads (r/atom [])
        this-elt (atom nil)
        update-threads!
        (fn [target-threads]
          (swap! threads
                 (fn [existing-threads]
                   ;; 'existing threads' are the threads currently displayed (via previous props)
                   ;; 'target threads' are the threads we want displayed (via latest props)
                   ;; 'target threads' likely overlap with 'existing threads'
                   ;; 'blank threads' are target threads that have no messages
                   (let [existing-thread-ids (set (map :id existing-threads))
                         target-thread-ids (set (map :id target-threads))
                         ;; possible to have multiple blank threads
                         ;; want only the 'new' ones to appear at front, rest stay as they were
                         new-blank-thread-ids (difference (->> target-threads
                                                               (filter (fn [thread]
                                                                         (empty? (thread :messages))))
                                                               (map :id)
                                                               (set))
                                                          existing-thread-ids)
                         to-remove (difference existing-thread-ids target-thread-ids)
                         to-add (difference target-thread-ids existing-thread-ids new-blank-thread-ids)
                         ;; here's the overall order:
                         ;;   blank threads (from target-threads)
                         ;;   existing threads (in their existing order) (minus any closed threads)
                         ;;   new threads (from target-threads)
                         ordered-ids (concat new-blank-thread-ids
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
         (update-threads! (props :threads))
         (reset! this-elt (r-dom/dom-node this)))

       :component-did-update
       (fn [this [_ prev-props]]
         (when (not= (set (map :id ((r/props this) :threads)))
                     (set (map :id (prev-props :threads))))
           (update-threads! ((r/props this) :threads))))

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
