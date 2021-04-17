(ns braid.search.ui.search-page
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [garden.core :as garden]
   [braid.search.client :refer [search-results-views]]))

(defn search-results-styles-view
  []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html
     (garden/css
       {:auto-prefix #{:transition
                       :flex-direction
                       :flex-shrink
                       :align-items
                       :animation
                       :flex-grow}
        :vendors ["webkit"]}
       (into [:.page.search>.search-results]
             (map :styles)
             (vals @search-results-views)))}}])

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
       [:div.description
        "Searching..."]]

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
       [search-results-styles-view]
       [:div.title
        (str "Search for \"" query "\"")
        (when (= status :loading) "  Loading...") ]
       [:div.search-results
        (doall
          (for [[type {:keys [view]}] (->> @search-results-views
                                           (sort-by (comp :priority second)))
                :when (seq (get results type))]
            ^{:key type}
            [view status (get results type)]))]]

      :done-empty
      [:div.page.search
       [:div.title (str "Search for \"" query "\"")]
       [:div.description "No results."]])))
