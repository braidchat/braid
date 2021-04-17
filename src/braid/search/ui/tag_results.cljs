(ns braid.search.ui.tag-results
  (:require
   [braid.core.client.ui.views.tag-hover-card :as tag-card]
   [braid.core.client.ui.styles.hover-cards :as card-styles]))

(defn search-tags-view
  [_ tags]
  [:div.result.tag
   [:div.description
    (str (count tags) " tags" (when (not= (count tags) 1) "s"))]
   [:div.tags.content
    (doall
      (for [{:keys [tag-id]} tags]
        ^{:key tag-id}
        [tag-card/tag-hover-card-view tag-id]))]])

(def styles
  [:>.result.tag
   [:>.tags.content
    ;; would like to just do "flex-wrap: wrap" but apparently a
    ;; flex-direction: column + flex-wrap: wrap doesn't grow the width
    ;; of the parent when it wraps, so then this ends up overlapping
    ;; with the search results next to it
    {:display "flex"
     :flex-direction "column"
     :align-items "flex-end"
     :max-height "90%"
     :gap "0.5rem"
     :margin-left "0.5rem"
     :overflow-y "auto"
     :padding "0.25rem"}
    card-styles/>tag-card
    [:>.card.tag
     {:min-height "3rem"
      :margin-top 0
      :margin-left "0.5rem"}]]])
