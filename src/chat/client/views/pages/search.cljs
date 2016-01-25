(ns chat.client.views.pages.search
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.threads :refer [thread-view]]))

(defn search-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page search"}
        (cond
          ; searching...
          (get-in data [:page :search-searching])
          (dom/div #js {:className "title"} "Searching...")

          ; results
          (seq (get-in data [:page :search-result-ids]))
          (apply dom/div #js {:className "threads"}
            (map (fn [t] (om/build thread-view t
                                   {:key :id
                                    :opts {:searched? true}}))
                 ; sort-by last reply, newest first
                 (->> (select-keys (data :threads) (get-in data [:page :search-result-ids]))
                      vals
                      (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              :messages))
                      reverse)))

          ; no results
          :else
          (dom/div #js {:className "title"} "No results"))))))
