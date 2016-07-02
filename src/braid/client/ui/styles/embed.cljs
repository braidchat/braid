(ns braid.client.ui.styles.embed
  (:require [garden.arithmetic :as m]
            [garden.units :refer [rem]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(defn embed [pad]
  [:.embed
   {:margin [[0 (m/* -1 pad)]]
    :padding [[pad 0 0]]
    :overflow "hidden"}

   [:.content
    {:cursor "pointer"}]

   [:.website
    {:background "black"
     :padding (m/* 0.5 pad)
     :color "white"
     :-webkit-font-smoothing "antialiased"
     :overflow "hidden"}

    [:.image
     {:width "75px"
      :height "75px"
      :float "left"
      :margin-right (m// pad 2)}]

    [:.about
     {:overflow "hidden"}

     [:.provider
      {:font-size "0.9em"
       :margin-bottom "0.5em"}

      [:.favicon
       {:display "inline-block"
        :width "1em"
        :height "1em"
        :background-repeat "no-repeat"
        :background-size "contain"
        :vertical-align "middle"
        :margin-right "0.25em"
        :white-space "nowrap"
        :max-width "10em"}]

      [:.name
       {:display "inline-block"
        :line-height "1em"
        :text-transform "uppercase"
        :vertical-align "middle"}]]

     [:.title
      {:font-weight "bold"}]

     [:.url
      {:font-size "0.9em"
       :margin-top "0.5em"
       :white-space "nowrap"}]]]

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
        :opacity 0.75 })]

    [:&:hover
     [:&:before
      {:opacity 1.0}]]]

   [:.image
    {:position "relative"}

    [:img
     {:width "100%"}]

    [:&:hover:before
     (mixins/fontawesome \uf14c)
     (let [font-size (rem 3)]
       {:font-size font-size
        :position "absolute"
        :top "50%"
        :left 0
        :right 0
        :text-align "center"
        :color "white"
        :display "block"
        :margin-top (m/* -0.5 font-size)})]]])
