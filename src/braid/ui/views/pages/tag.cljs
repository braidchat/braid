(ns braid.ui.views.pages.tag
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.thread :refer [thread-view]]
            [braid.ui.views.new-thread :refer [new-thread-view]]
            [braid.ui.views.pills :refer [tag-pill-view subscribe-button-view]]))

(defn tag-page-view
  []
  (let [loading? (r/atom false)
        start-loading! (fn [] (reset! loading? true))
        stop-loading! (fn [] (reset! loading? false))
        page-id (subscribe [:page-id])
        page (subscribe [:page])
        tag (subscribe [:tag @page-id])
        tag-id (@page :id)
        user-id (subscribe [:user-id])
        threads (subscribe [:threads])
        pagination-remaining (subscribe [:pagination-remaining])
        inbox-thread-ids (subscribe [:open-thread-ids])
        known-threads (reaction
                        (->> @threads
                               vals
                               (filter
                                 (fn [t] (contains? (set (t :tag-ids)) tag-id)))))
        sorted-threads (reaction
                         (->> (@page :thread-ids)
                            (select-keys @threads)
                            vals
                            (into (set @known-threads))
                            (map (fn [t] (assoc t :open? (contains? @inbox-thread-ids (t :id)))))
                            ; sort-by last reply, newest first
                            (sort-by
                              (comp (partial apply max)
                                    (partial map :created-at)
                                    :messages))
                            reverse))]
    (r/create-class
      {:component-did-mount
       (fn []
         (dispatch! :threads-for-tag {:tag-id @page-id}))
       :reagent-render
       (fn []
         (let
           [status (cond
                     loading? :loading
                     (not (contains? @page :thread-ids)) :searching
                     (seq (@page :thread-ids)) :done-results
                     :else :done-empty)]
            [:div.page.channel
              [:div.title
                [tag-pill-view @tag]
                [subscribe-button-view @tag]]

              [:div.content
                [:div.description
                  [:p "One day, a tag description will be here."]

                  [:div
                    (case status
                      :searching "Searching..."
                      :loading "Loading more..."
                      :done-results (str "Done! Displaying " (count @sorted-threads)
                                         " out of " (+ (count @sorted-threads) @pagination-remaining))
                      :done-empty "No Results")]]]


              [:div.threads {:ref "threads-div"
                            :on-scroll ; page in more results as the user scrolls
                              (fn [e]
                                (let [div (.. e -target)]
                                  (when (and (= status :done-results)
                                          (> @pagination-remaining 0)
                                          (> 100 (- (.-scrollWidth div)
                                                    (+ (.-scrollLeft div) (.-offsetWidth div)))))
                                    (start-loading!)
                                    (dispatch! :threads-for-tag
                                               {:tag-id @page-id
                                                :offset (count threads)
                                                :on-complete
                                                (fn []
                                                  (stop-loading!))}))))
                            :on-wheel ; make the mouse wheel scroll horizontally
                              (fn [e]
                                (let [target-classes (.. e -target -classList)
                                      ;TODO: properly reference threads-div for scroll
                                      this-elt (.. e -target)]
                                  ; TODO: check if threads-div needs to scroll?
                                  (when (and (or (.contains target-classes "thread")
                                                 (.contains target-classes "threads"))
                                          (= 0 (.-deltaX e) (.-deltaZ e)))
                                    (set! (.-scrollLeft this-elt)
                                          (- (.-scrollLeft this-elt) (.-deltaY e))))))}
                  [new-thread-view]
                  (doall
                    (for [thread @sorted-threads]
                      ^{:key [thread :id]}
                      [thread-view thread]))]]))})))
