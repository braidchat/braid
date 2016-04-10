(ns chat.client.views.pages.search
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [thread-view]]))

(defn search-page-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:loading? false})
    om/IRenderState
    (render-state [_ {:keys [loading?]}]
      (let [page (data :page)
            status (cond
                     loading? :loading
                     (not (contains? page :thread-ids)) :searching
                     (seq (page :thread-ids)) :done-results
                     :else :done-empty)]
        (dom/div #js {:className "page search"}
          (dom/div #js {:className "title"} "Search")
          (case status
            :searching
            (dom/div #js {:className "content"}
              (dom/div #js {:className "description"}
                "Searching..."))

            (:done-results :loading)
            (let [threads (vals (select-keys (data :threads) (page :thread-ids)))]
              (dom/div #js {:className "content"}
                (dom/div #js {:className "description"}
                  (if (= status :loading)
                    "Loading more results..."
                    (str "Displaying " (count threads) "/" (count (page :thread-ids)))))
                (apply dom/div #js {:className "threads"
                                    :ref "threads-div"
                                    :onScroll ; page in more results as the user scrolls
                                    (fn [e]
                                      (let [div (.. e -target)]
                                        (when (and (= status :done-results)
                                                (< (count threads) (count (page :thread-ids)))
                                                (> 100 (- (.-scrollWidth div)
                                                          (+ (.-scrollLeft div) (.-offsetWidth div)))))
                                          (om/set-state! owner :loading? true)
                                          (let [already-have (set (map :id threads))
                                                to-load (->> (page :thread-ids)
                                                             (remove already-have)
                                                             (take 25))]
                                            (dispatch! :load-threads
                                                       {:thread-ids to-load
                                                        :on-complete
                                                        (fn []
                                                          (om/set-state! owner :loading? false))})))))
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
                  (map (fn [t] (om/build thread-view t
                                         {:key :id}))
                       ; sort-by last reply, newest first
                       (->> threads
                            (sort-by
                              (comp (partial apply max)
                                    (partial map :created-at)
                                    :messages))
                            reverse)))))

            :done-empty
            (dom/div #js {:className "content"}
              (dom/div #js {:className "description"}
                "No results."))))))))
