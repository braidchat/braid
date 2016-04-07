(ns braid.ui.styles.embed
  (:require [garden.arithmetic :as m]
            [braid.ui.styles.mixins :as mixins]
            [garden.units :refer [rem]]))

(defn embed [pad]
  [:.embed
   {:margin [[pad (m/* -1 pad)]]}
   [:.content
    {:cursor "pointer"}]
   [:.website
    [:.favicon
     {:width "20px"
      :height "20px"}]
    [:img
     {:width "75px"
      :height "75px"
      :float "left"}]
    [:.title
     {:font-weight "bold"}]]
   [:.video
    {:position "relative"}
    [:img
     {:width "100%"}]
    [:&:before
     (mixins/fontawesome \uf04b)
     (let [font-size (rem 3)]
       {:font-size font-size
        :position "absolute"
        :top "50%"
        :left 0
        :right 0
        :text-align "center"
        :color "white"
        :display "block"
        :margin-top (m/* -0.5 font-size)
        :opacity 0.75
        })]
    [:&:hover
     [:&:before
      {:opacity 1.0}]]


    ]])
