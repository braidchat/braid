(ns chat.client.views.pages.channel
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]))

(defn channel-page-view [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (dispatch! :threads-for-tag {:tag-id (get-in data [:page :id])}))
    om/IInitState
    (init-state [_]
      {:loading? false})
    om/IRenderState
    (render-state [_ {:keys [loading?]}]
      (let [page (data :page)
            tag-id (page :id)
            tag (get-in data [:tags tag-id])
            user-id (get-in data [:session :user-id])
            status (cond
                     loading? :loading
                     (not (contains? page :thread-ids)) :searching
                     (seq (page :thread-ids)) :done-results
                     :else :done-empty)
            known-threads (->> (data :threads)
                               vals
                               (filter (fn [t] (contains? (set (t :tag-ids)) tag-id))))

            inbox-thread-ids (get-in @store/app-state [:user :open-thread-ids])
            threads (->> (page :thread-ids)
                         (select-keys (data :threads))
                         vals
                         (into (set known-threads))
                         (map (fn [t] (assoc t :open? (contains? inbox-thread-ids (t :id)))))
                         ; sort-by last reply, newest first
                         (sort-by
                           (comp (partial apply max)
                                 (partial map :created-at)
                                 :messages))
                         reverse)]
        (dom/div #js {:className "page channel"}
          (dom/div #js {:className "title"}
            (om/build tag-view tag)
            (subscribe-button tag))

          (dom/div #js {:className "content"}
            (dom/div #js {:className "description"}
              (dom/p nil "One day, a channel description will be here.")

              (dom/div nil
                (case status
                  :searching "Searching..."
                  :loading "Loading more..."
                  :done-results (str "Done! Displaying " (count threads)
                                     " out of " (+ (count threads) (store/pagination-remaining)))
                  :done-empty "No Results"))))


          (apply dom/div #js {:className "threads"
                              :ref "threads-div"
                              :onScroll ; page in more results as the user scrolls
                              (fn [e]
                                (let [div (.. e -target)]
                                  (when (and (= status :done-results)
                                          (> (store/pagination-remaining) 0)
                                          (> 100 (- (.-scrollWidth div)
                                                    (+ (.-scrollLeft div) (.-offsetWidth div)))))
                                    (om/set-state! owner :loading? true)
                                    (dispatch! :threads-for-tag
                                               {:tag-id (get-in data [:page :id])
                                                :offset (count threads)
                                                :on-complete
                                                (fn []
                                                  (om/set-state! owner :loading? false))}))))
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
              [(new-thread-view {:tag-ids [tag-id]})]
              (map (fn [t] (om/build thread-view t {:key :id}))
                   threads))))))))
