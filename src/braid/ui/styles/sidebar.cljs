(ns braid.ui.styles.sidebar
  (:require [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]
            [braid.ui.styles.mixins :as mixins]
            [braid.ui.styles.vars :as vars]))

(def badge
  [:.badge
   mixins/pill-box
   [:&
    {:font-size (rem 0.6)
     :background "#B53737 !important"
     :color "white"
     :border-color "#B53737 !important"
     :position "absolute"
     :bottom (rem -0.5)
     :right (rem -0.5)}]])

(def sidebar
  [:.sidebar
   {:background "#222"
    :padding vars/pad
    :overflow-x "visible"
    :overflow-y "auto"}

   [:.option
    {:width vars/top-bar-height
     :height vars/top-bar-height
     :border-radius (m// vars/pad 3)
     :display "block"
     :text-decoration "none"
     :line-height vars/top-bar-height
     :text-align "center"
     :font-size (em 1.5)}

    (let [w (m// vars/pad 3)]
      [:&.active:before
       {:content "\"\""
        :background "#eee"
        :width w
        :height "100%"
        :position "absolute"
        :left (m/- vars/pad)
        :border-radius [[0 w w 0]]}])

    [:&.group
     {:margin [[0 0 vars/pad 0]]
      :color "#222"
      :position "relative"}]

    [:&.plus
     {:background "#333"
      :color "#222"
      :font-family "fontawesome"}

     [:&:after
      (mixins/fontawesome \uf067)]

     [:&:hover
      {:background "#555"}]]

    badge]])
