(ns braid.ui.views.pages.search
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view]]))

(defn search-page-view
  [data owner]
  (let [loading? (r/atom false)
        start-loading! (fn [] (swap! loading? true))
        stop-loading! (fn [] (swap! loading? false))]
    (fn []
      (let [page (data :page)
            status (cond
                     loading? :loading
                     (not (contains? page :thread-ids)) :searching
                     (seq (page :thread-ids)) :done-results
                     :else :done-empty)]
        [:div.page.search
          [:div.title "Search"]
          (case status
            :searching
            [:div.content
              [:div.description
                "Searching..."]]

            (:done-results :loading)
            (let [threads (vals (select-keys (data :threads) (page :thread-ids)))]
              [:div.content
                [:div.description
                  (if (= status :loading)
                    "Loading more results..."
                    (str "Displaying " (count threads) "/" (count (page :thread-ids))))]
                [:div.threads {:ref "threads-div"
                               :on-scroll ; page in more results as the user scrolls
                               (fn [e]
                                 (let [div (.. e -target)]
                                   (when (and (= status :done-results)
                                           (< (count threads) (count (page :thread-ids)))
                                           (> 100 (- (.-scrollWidth div)
                                                     (+ (.-scrollLeft div) (.-offsetWidth div)))))
                                     (start-loading!)
                                     (let [already-have (set (map :id threads))
                                           to-load (->> (page :thread-ids)
                                                        (remove already-have)
                                                        (take 25))]
                                       (dispatch! :load-threads
                                                  {:thread-ids to-load
                                                   :on-complete
                                                   (fn []
                                                     (stop-loading!))})))))
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
                  (for [thread threads]
                    (om/build thread-view thread {:key :id}))
                       ; sort-by last reply, newest first
                       (->> threads
                            (sort-by
                              (comp (partial apply max)
                                    (partial map :created-at)
                                    :messages))
                            reverse)]])

            :done-empty
            [:div.content
              [:div.description
                "No results."]])]))))
