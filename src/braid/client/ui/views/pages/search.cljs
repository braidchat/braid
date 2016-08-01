(ns braid.client.ui.views.pages.search
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction run!]]
            [braid.client.ui.views.thread :refer [thread-view]]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]]))

(defn search-page-view
  []
  (let [page (subscribe [:page])
        threads (subscribe [:threads])
        query (subscribe [:search-query])
        group-id (subscribe [:open-group-id])]
    (fn []
      (let [status (cond
                     (@page :error?) :error
                     (@page :loading?) :loading
                     (not (contains? @page :thread-ids)) :searching
                     (seq (@page :thread-ids)) :done-results
                     :else :done-empty)]

        [:div.page.search
         [:div.title (str "Search for \"" @query "\"")]
         (case status
           :searching
           [:div.content
            [:div.description
             "Searching..."]]

           :error
           [:div.content
            [:p "Search timed out"]
            [:button {:on-click (fn [_]
                                  (dispatch! :search-history [(@page :search-query) @group-id]))}
             "Try again"]]

           (:done-results :loading)
           (let [loaded-threads (reaction (vals (select-keys @threads (@page :thread-ids))))
                 sorted-threads (reaction (->> @loaded-threads ; sort-by last reply, newest first
                                               (sort-by
                                                 (comp (partial apply max)
                                                       (partial map :created-at)
                                                       :messages))
                                               reverse))]
             [:div.content
              [:div.description
               (if (= status :loading)
                 "Loading more results..."
                 (str "Displaying " (count @loaded-threads) "/" (count (@page :thread-ids))))]
              [:div.threads
               {:ref "threads-div"
                :on-scroll ; page in more results as the user scrolls
                (fn [e]
                  (let [div (.. e -target)]
                    (when (and (= status :done-results)
                            (< (count @loaded-threads) (count (@page :thread-ids)))
                            (> 100 (- (.-scrollWidth div)
                                      (+ (.-scrollLeft div) (.-offsetWidth div)))))
                      (dispatch! :set-page-loading true)
                      (let [already-have (set (map :id @loaded-threads))
                            to-load (->> (@page :thread-ids)
                                         (remove already-have)
                                         (take 25))]
                        (dispatch! :load-threads
                                   {:thread-ids to-load
                                    :on-complete
                                    (fn []
                                      (dispatch! :set-page-loading false))})))))
                :on-wheel ; make the mouse wheel scroll horizontally
                (fn [e]
                  (let [target-classes (.. e -target -classList)
                        this-elt (.. e -target)]
                    ; TODO: check if threads-div needs to scroll?
                    (when (and (or (.contains target-classes "thread")
                                   (.contains target-classes "threads"))
                            (= 0 (.-deltaX e) (.-deltaZ e)))
                      (set! (.-scrollLeft this-elt)
                            (- (.-scrollLeft this-elt) (.-deltaY e))))))}
               (doall
                 (for [thread @sorted-threads]
                   ^{:key (:id thread)}
                   [thread-view thread]))]])

           :done-empty
           [:div.content
            [:div.description "No results."]])]))))
