(ns braid.ui.styles.embed
  (:require [garden.arithmetic :as m]
            [garden.units :refer [rem]]
            [braid.ui.styles.mixins :as mixins]
            [braid.ui.styles.vars :as vars]))

(defn embed [pad]
  [:.embed
   {:margin [[pad (m/* -1 pad)]]}

   [:.content
    {:cursor "pointer"}]

   [:.website

    [:.image
     {:width "75px"
      :height "75px"
      :float "left"
      :margin-right (m// pad 2)}]

    [:.about

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
        :margin-right "0.25em"}]

      [:.name
       {:display "inline-block"
        :line-height "1em"
        :text-transform "uppercase"
        :vertical-align "middle"
        :color vars/grey-text}]]

     [:.title
      {:font-weight "bold"}]

     [:.url
      {:font-size "0.9em"
       :color vars/grey-text
       :margin-top "0.5em"}]]]

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

    [:img
     {:width "100%"}]]])
