(ns braid.core.client.ui.views.threads
  (:require
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [clojure.set :refer [difference]]
   [reagent.core :as r]
   [reagent.dom :as r-dom]))

(defn threads-view
  "
  threads
     threads to display;
     component will 'cache' order of threads if new threads come in,
     to prevent threads from 'jumping around'

  resort-nonce (optional, but recommended)
    value of any type, if it changes, will 'force' re-render threads
    in the order as passed in (ie. without caching)

  new-thread-view (optional)
    hiccup to include before threads

  thread-order-dirty? (optional)
    atom, will be reset! to true when displayed thread order does not match passed-in order"
  [{:keys [threads
           new-thread-view
           resort-nonce
           thread-order-dirty?] :as props}]
  ;; to avoid accidental re-use when switching groups, key this component with the group-id

  ;; for a better user experience, this component maintains order of threads
  ;; after mounting, any new threads that come in via props are appended to the end of existing threads
  ;; (except blank new threads, which are put at the front)
  (let [threads (r/atom [])
        this-elt (atom nil)
        reset-threads!
        (fn [target-threads]
          (reset! threads target-threads))
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
         (reset-threads! (props :threads))
         (reset! this-elt (r-dom/dom-node this)))

       :component-did-update
       (fn [this [_ prev-props]]
         (let [target-threads ((r/props this) :threads)]
           (cond
             (not= ((r/props this) :resort-nonce)
                   (prev-props :resort-nonce))
             (reset-threads! target-threads)

             (not= (set (map :id target-threads))
                   (set (map :id (prev-props :threads))))
             (update-threads! target-threads))
           ;; thread-order-dirty is optional
           (when thread-order-dirty?
             (reset! thread-order-dirty? (not= (map :id @threads)
                                               (map :id target-threads))))))

       :reagent-render
       (fn [{:keys [new-thread-view]}]
         [:div.threads
          {:on-wheel scroll-horizontally}

          (when new-thread-view
            new-thread-view)

          (doall
            (for [thread @threads]
              ^{:key (thread :id)}
              [thread-view (thread :id)]))])})))
