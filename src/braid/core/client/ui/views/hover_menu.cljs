(ns braid.core.client.ui.views.hover-menu
  (:require
    [braid.core.client.ui.views.header-item :refer [header-item-view]]))

(defn hover-menu-view
  [items position]
  [:div.hover-menu {:class (name position)}
   [:svg.arrow {:view-box "0 -5 10 10"
                :width "10px"
                :height "10ox"}
    [:polygon {:points "0,-5 0,5, 6,0"
               :transform "rotate(90 0 5)"
               :fill "white"
               :stroke "#333"
               :stroke-width "0.5px"}]]
   (into
     [:div.content]
     (doall
       (for [item (->> items
                       (sort-by :priority)
                       reverse)]
         ^{:key (item :class)}
         [header-item-view item])))])
