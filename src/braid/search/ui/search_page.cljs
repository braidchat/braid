(ns braid.search.ui.search-page
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [braid.search.client :refer [search-results-views]]))

(defn search-page-view
  []
  (let [{:keys [query results
                error? loading?]} @(subscribe [:braid.search/state])
        status (cond
                 error? :error
                 loading? :loading
                 (nil? results) :searching
                 (seq results) :done-results
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
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.content
        [:div.description
         (when (= status :loading)
           "Loading more results...")]]
       (doall (for [[type view] @search-results-views]
                ^{:key type}
                [view status (get results type)]))]

      :done-empty
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.content
        [:div.description "No results."]]])))
