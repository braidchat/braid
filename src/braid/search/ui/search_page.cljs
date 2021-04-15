(ns braid.search.ui.search-page
  (:require
   [clojure.set :as set]
   [re-frame.core :refer [dispatch subscribe]]
   [braid.core.client.ui.views.thread :refer [thread-view]]))

(defn search-page-view
  []
  (let [{:keys [query thread-ids
                error? loading?]} @(subscribe [:braid.search/state])
        status (cond
                 error? :error
                 loading? :loading
                 (nil? thread-ids) :searching
                 (seq thread-ids) :done-results
                 :else :done-empty)]
    (case status
      :searching
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.content
        [:div.description
         "Searching..."]]]

      :error
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.content
        [:p "Search timed out"]
        [:button
         {:on-click
          (fn [_]
            (dispatch [:braid.search/search-history! query @(subscribe [:open-group-id])]))}
         "Try again"]]]

      (:done-results :loading)
      (let [threads @(subscribe [:threads])
            loaded-threads (vals (select-keys threads thread-ids))
            sorted-threads (->> loaded-threads
                                ;; sort-by last reply, newest first
                                (sort-by
                                  (comp (partial apply max)
                                        (partial map :created-at)
                                        :messages)
                                  #(compare %2 %1)))
            maybe-load-more (fn []
                              (when (and (= status :done-results)
                                         (< (count loaded-threads) (count thread-ids)))
                                (dispatch [:set-page-loading! true])
                                (let [already-have (set (map :id loaded-threads))
                                      to-load (->> thread-ids
                                                   (remove already-have)
                                                   (take 25))]
                                  (dispatch [:load-threads!
                                             {:thread-ids to-load
                                              :on-complete
                                              (fn []
                                                (dispatch [:set-page-loading! false]))}]))))]
        (when (< (count (set/intersection (into #{} (map :id)
                                                loaded-threads)
                                          (set thread-ids)))
                 (min 25 (count thread-ids)))
          (maybe-load-more))
        [:div.page.search
         [:div.title (str "Search for \"" query "\"")]
         [:div.content
          [:div.description
           (if (= status :loading)
             "Loading more results..."
             (str "Displaying " (count loaded-threads) "/"
                  (count thread-ids)))]]
         [:div.threads
          {:on-scroll ; page in more results as the user scrolls
           (fn [e]
             (let [div (.. e -target)]
               (when (and (= (.-className div) "threads")
                          (> 100 (- (.-scrollWidth div)
                                    (+ (.-scrollLeft div) (.-offsetWidth div)))
                             0))
                 (maybe-load-more))))
           :on-wheel ; make the mouse wheel scroll horizontally
           (fn [e]
             (let [target-classes (.. e -target -classList)
                   this-elt (.. e -target)]
               ;; TODO: check if threads-div needs to scroll?
               (when (and (or (.contains target-classes "thread")
                              (.contains target-classes "threads"))
                       (= 0 (.-deltaX e) (.-deltaZ e)))
                 (set! (.-scrollLeft this-elt)
                       (- (.-scrollLeft this-elt) (.-deltaY e))))))}
          (doall
            (for [thread sorted-threads]
              ^{:key (:id thread)}
              [thread-view (:id thread)]))]])

      :done-empty
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.content
        [:div.description "No results."]]])))
