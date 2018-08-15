(ns braid.core.client.ui.styles.hover-menu
  (:require
    [garden.arithmetic :as m]
    [braid.core.client.ui.styles.mixins :as mixins]
    [braid.core.client.ui.styles.vars :as vars]))

(defn >hover-menu []
  [:>.hover-menu
   {:background "white"
    :border "0.5px solid #333"
    :border-radius vars/border-radius}
   (mixins/box-shadow)

   [:.content
    {:overflow-x "auto"
     :height "100%"
     :box-sizing "border-box"
     :padding [[(m/* vars/pad 0.75)]]}]

   ; triangle
   [:&::before
    (mixins/fontawesome \uf0d8)
    {:position "absolute"
     :top "-0.65em"
     :right (m/* vars/pad 0.70)
     :color "white"
     :font-size "1.5em"}]])

