(ns braid.core.client.ui.styles.reconnect-overlay
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.arithmetic :as m]))

(def reconnect-overlay
  [:>.reconnect-overlay
   {:position "absolute"
    :bottom 0
    :left 0
    :right 0
    :top 0
    :background "rgba(242,242,242,0.75)"
    :z-index 5000}

   [:>.info
    {:position "absolute"
     :bottom vars/pad
     :height (m/* vars/pad 4)
     :left 0
     :right 0
     :padding vars/pad
     :box-sizing "border-box"
     :background "red"
     :color "white"
     :z-index 5001}

    [:>h1
     {:font-size "1em"
      :margin 0}]

    [:>.message

     [:>button
      (mixins/outline-button {:text-color "#fff"
                              :border-color "#fff"
                              :hover-text-color "#fff"
                              :hover-border-color "fff"})]]]])
