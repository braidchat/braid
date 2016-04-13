(ns braid.ui.views.pages.channel
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view new-thread-view]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]))

(defn channel-page-view-test
  [data subscribe]
  (let [loading? (r/atom false)
        start-loading! (fn [] (swap! loading? true))
        stop-loading! (fn [] (swap! loading? false))]
    (r/create-class
      {:component-did-mount
       (fn []
         (dispatch! :threads-for-tag {:tag-id (get-in data [:page :id])}))
       :reagent-render
       (fn []
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
            inbox-thread-ids (subscribe [:open-thread-ids])
            threads (->> (page :thread-ids)
                         (select-keys (data :threads))
                         vals
                         (into (set known-threads))
                         (map (fn [t] (assoc t :open? (contains? @inbox-thread-ids (t :id)))))
                         ; sort-by last reply, newest first
                         (sort-by
                           (comp (partial apply max)
                                 (partial map :created-at)
                                 :messages))
                         reverse)]
        [:div.page.channel
          [:div.title
            (om/build tag-view tag)
            (subscribe-button tag)]

          [:div.content
            [:div.description
              [:p "One day, a channel description will be here."]

              [:div
                (case status
                  :searching "Searching..."
                  :loading "Loading more..."
                  :done-results (str "Done! Displaying " (count threads)
                                     " out of " (+ (count threads) (store/pagination-remaining)))
                  :done-empty "No Results")]]]


          [:div.threads {:ref "threads-div"
                        :on-scroll ; page in more results as the user scrolls
                        (fn [e]
                          (let [div (.. e -target)]
                            (when (and (= status :done-results)
                                    (> (store/pagination-remaining) 0)
                                    (> 100 (- (.-scrollWidth div)
                                              (+ (.-scrollLeft div) (.-offsetWidth div)))))
                              (start-loading!)
                              (dispatch! :threads-for-tag
                                         {:tag-id (get-in data [:page :id])
                                          :offset (count threads)
                                          :on-complete
                                          (fn []
                                            (stop-loading!))}))))
                        :on-wheel ; make the mouse wheel scroll horizontally
                        (fn [e]
                          (let [target-classes (.. e -target -classList)
                                this-elt (om/get-node owner "threads-div")]
                            ; TODO: check if threads-div needs to scroll?
                            (when (and (or (.contains target-classes "thread")
                                           (.contains target-classes "threads"))
                                    (= 0 (.-deltaX e) (.-deltaZ e)))
                              (set! (.-scrollLeft this-elt)
                                    (- (.-scrollLeft this-elt) (.-deltaY e))))))}
              [(new-thread-view {:tag-ids [tag-id]})]
              (for [thread threads]
                (om/build thread-view thread {:key :id}))]]))})))

