(ns chat.client.views.pages.search
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view]]))

(defn search-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [page (data :page)
            status (cond
                     (not (contains? page :thread-ids)) :searching
                     (seq (page :thread-ids)) :done-results
                     :else :done-empty)]
        (dom/div #js {:className "page search"}
          (dom/div #js {:className "title"} "Search")
          (case status
            :searching
            (dom/div #js {:className "description"} "Searching...")

            :done-results
            (dom/div nil
              (apply dom/div #js {:className "threads"}
                (map (fn [t] (om/build thread-view t
                                       {:key :id}))
                     ; sort-by last reply, newest first
                     (->> (select-keys (data :threads) (page :thread-ids))
                          vals
                          (sort-by
                            (comp (partial apply max)
                                  (partial map :created-at)
                                  :messages))
                          reverse))))

            :done-empty
            (dom/div #js {:className "description"} "No results.")))))))
