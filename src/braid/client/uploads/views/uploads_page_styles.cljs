(ns braid.client.uploads.views.uploads-page-styles
  (:require [garden.units :refer [rem em]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def uploads-page
  [:.page.uploads
   [:.uploads
    {:width "100%"
     :flex-direction "row"
     :flex-wrap "wrap"
     :align-content "space-between"
     :align-item "bottom"}
    [:.upload
     [:.embed
      {:width (rem 5)
       :margin 0}]
     [:.uploaded-thread
      [:.tags
       {:display "inline"}]]
     {:height (rem 3)
      :margin (em 1)}]]])
