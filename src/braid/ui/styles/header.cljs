(ns braid.ui.styles.header
  (:require [garden.units :refer [em]]
            [braid.ui.styles.vars :as vars]
            [braid.ui.styles.mixins :as mixins]))

(defn header [pad]
  [:.app
   [:.main
    ["> .header"
     {:position "absolute"
      :top vars/pad
      :right vars/pad
      :z-index 100
      :line-height vars/top-bar-height
      :color vars/grey-text}

     [:.modal
      {:display "none"
       :background "white"
       :position "absolute"
       :cursor "default"}
      [:p
       {:line-height (em 1.2)}]]

     [:.clear-inbox
      {:display "inline-block"
       :margin-right vars/pad
       :vertical-align "top"}
      [:button
       mixins/pill-button]]

     [:.shortcut
      {:display "inline-block"
       :margin-right vars/pad}

      [:.title
       {:text-decoration "none"
        :color vars/grey-text}]

      [:&.active :&:hover
       [:.title
        {:color "#000"}]]

      [:&:hover
       [:.modal
        {:display "block"
         :padding vars/pad}]]]

     [:.inbox
      [:.title:after
       (mixins/fontawesome \uf01c)]]

     [:.recent
      [:.title:after
       (mixins/fontawesome \uf1da)]]

     [:.help
      [:.title:after
       (mixins/fontawesome \uf059)]]

     [:.users
      [:.title:after
       (mixins/fontawesome \uf007)
       {:margin-left (em 0.25)}]]

     [:.tags
      [:.title:after
       (mixins/fontawesome \uf02c)]]

     [:.extensions
      [:.title:after
       (mixins/fontawesome \uf1e6)]]


     [:.search-bar
      {:display "inline-block"}

      [:input
       {:width (em 20)}]]

     [:.avatar
      {:width vars/top-bar-height
       :height vars/top-bar-height
       :vertical-align "middle"
       :margin-left vars/pad
       :border-radius "20%"}]]]])
