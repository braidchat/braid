(ns braid.core.client.ui.styles.hover-menu
  (:require
    [garden.arithmetic :as m]
    [garden.units :refer [px]]
    [braid.core.client.ui.styles.mixins :as mixins]
    [braid.core.client.ui.styles.vars :as vars]))

(defn >hover-menu []
  [:>.hover-menu
   {:background "white"
    :border "0.5px solid #ddd"
    :border-radius vars/border-radius
    ;; use filter instead of box-shadow
    ;; so that it takes the shape of the arrow too
    :filter "drop-shadow(0 1px 2px #ccc)"}

   [:&.top
    {:position "absolute"
     :bottom "100%"}]

   [:&.bottom
    {:position "absolute"
     :top "100%"}]

   [:&.left
    {:position "absolute"
     :right "100%"}]

   [:&.right
    {:position "absolute"
     :left "100%"}]

   ;; arrow
   (let [size (px 10)]
     [:>.arrow
      {:position "absolute"
       :width size
       :height size
       :fill "white"
       :stroke "#ddd"
       :stroke-width "0.51"}

      [:&.top
       {:bottom (m/- size)
        :left "11px"}]

      [:&.bottom
       {:top (m/- size)
        :left "11px"}]

      [:&.left
       {:right (m/- size)
        :top "11px"}]

      [:&.right
       {:left (m/- size)
        :top "11px"}]])

   [:.content
    {:overflow-x "auto"
     :height "100%"
     :box-sizing "border-box"
     :padding [[(m/* vars/pad 0.75)]]}

    [:>a
     {:display "block"
      :color "black"
      :text-align "right"
      :text-decoration "none"
      :line-height "1.85em"
      :white-space "nowrap"
      :cursor "pointer"}

     [:&:hover
      {:color "#666"}]

     [:>.icon
      {:font-family "fontawesome"
       :display "inline-block"
       :margin-left "0.5em"
       :width "1.5em"}]]]])

