(ns braid.search.ui.search-page-styles)

(def >search-page
  [:>.page.search
   [:>.title
    {:position "absolute"
     :top "0"}]
   [:>.search-results
    {:position "absolute"
     :bottom 0
     :display "flex"
     :height "100%"
     :flex-direction "row"
     :align-items "flex-end"}
    [:>.result
     {:position "relative"
      :min-width "min-content"
      :display "flex"
      :align-items "flex-end"
      :height "90%"}
     [:>.description
      {:position "absolute"
       :top "1rem"
       :left "1rem"}]]]])
