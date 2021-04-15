(ns braid.search.ui.search-page-styles
  (:require
   [braid.core.client.ui.styles.threads :as threads-style]))

(def >search-page
  [:>.page.search
   #_{:display "flex"
    :flex-direction "row"}
   [:>.results
    {:display "flex"
     :height "100%"
     :flex-direction "row"
     :position "relative"}
    (threads-style/>threads)]])
