(ns braid.core.client.ui.views.hover-menu
  (:require
    [braid.core.client.ui.views.header-item :refer [header-item-view]]))

(defn hover-menu-view
  [context items position]
  [:div.hover-menu {:class (name position)}
   [:svg.arrow {:class (name position)
                :view-box "0 -5 10 10"}
    [:polyline {:points "0,-5 5,0 0,5"
                :transform (str "rotate("
                                (case position
                                  :top 90 ;
                                  :bottom 270
                                  :left 0
                                  :right 180)
                                ", 5, 0)")}]]
   (into
     [:div.content]
     (doall
       (for [item (->> items
                       (sort-by :priority)
                       reverse)]
         ^{:key (item :class)}
         [header-item-view (assoc item :context context)])))])
