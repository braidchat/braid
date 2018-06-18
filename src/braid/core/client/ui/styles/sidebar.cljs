(ns braid.core.client.ui.styles.sidebar
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.arithmetic :as m]
   [garden.units :refer [rem em px ex]]))

(def badge
  [:>.badge
   mixins/pill-box

   [:&
    {:font-size (rem 0.6)
     :background "#B53737 !important"
     :color "white"
     :border-color "#B53737 !important"
     :position "absolute"
     :bottom (rem -0.5)
     :right (rem -0.5)}]])

(defn sidebar-button [size]
  [:&
   {:width size
    :height size
    :border-radius (m// vars/pad 3)
    :display "block"
    :text-decoration "none"
    :line-height size
    :text-align "center"
    :font-size (em 1.5)
    :opacity 0.5
    :position "relative"}

   [:&:hover
    {:opacity 1}]

   [:&.active
    {:opacity 1}

    (let [w (m// vars/pad 3)]
      [:&::before
       {:content "\"\""
        :background "#eee"
        :width w
        :height "100%"
        :position "absolute"
        :left (m/- vars/pad)
        :border-radius [[0 w w 0]]}])]

   badge])

(def sidebar
  [:>.sidebar
   {:position "fixed"
    :left 0
    :top 0
    :bottom 0
    :width vars/sidebar-width
    :box-sizing "border-box"
    :background "#222"
    :padding vars/pad
    :overflow-x "visible"
    :overflow-y "auto"
    :display "flex"
    :flex-direction "column"
    :z-index 9999}

   [:>.groups
    [:>.group
     (sidebar-button vars/top-bar-height)
     {:margin [[0 0 vars/pad 0]]
      :color "#222"
      :box-shadow [[0 (px 1) (px 4) 0 "rgba(0,0,0,0.5)"]]}]]

   [:>.spacer
    {:flex-grow 2}]

   [:>.global-settings
    :>.plus
    :>.login
    (sidebar-button vars/top-bar-height)
    {:color "#999"}

    [:&::after
     {:font-size (em 1.25)
      :-webkit-font-smoothing "antialias"}]

    [:&:hover
     {:color "#FFF"}]

    [:&.plus::after
     (mixins/fontawesome \uf067)]

    [:&.global-settings::after
     (mixins/fontawesome \uf013)]

    [:&.login::after
     (mixins/fontawesome \uf090)]]])
