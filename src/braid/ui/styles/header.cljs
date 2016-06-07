(ns braid.ui.styles.header
  (:require [garden.units :refer [em px]]
            [garden.arithmetic :as m]
            [braid.ui.styles.vars :as vars]
            [braid.ui.styles.mixins :as mixins]))

(def header-height vars/avatar-size)

(defn header [pad]
  [:.app
   [:.main
    ["> .header"
     {:position "absolute"
      :left vars/sidebar-width
      :top vars/pad
      :margin-left vars/pad
      :z-index 100
      :border-radius vars/border-radius
      :overflow "hidden"
      :height header-height}
     (mixins/box-shadow)

     [:a
      {:display "inline-block"
       :vertical-align "top"
       :padding [[0 (m/* vars/pad 0.5)]]
       :color "white"
       :text-decoration "none"
       :background "black"
       :height header-height
       :line-height header-height
       :-webkit-font-smoothing "antialiased"}
       
      [:&:hover
       {:background "#666"}]

      [:&.group-name
       {:display "inline-block"
        :text-transform "uppercase"
        :padding-right (m/* vars/pad 0.25)
        :padding-left (m/* vars/pad 0.75)
        :letter-spacing "0.1em"
        :font-weight "bold"}]

      [:&.inbox:after
       (mixins/fontawesome \uf01c)]

      [:&.recent:after
       (mixins/fontawesome \uf1da)]]

     [:.search
      {:display "inline-block"}

      [:input
       {:border 0
        :padding-left vars/pad
        :min-width "15em"
        :width "25vw"
        :height header-height
        :outline "none"}]

       [:&:after
        {:margin-left "-2em"
         :color "#ccc"}
        (mixins/fontawesome \uf002)]]]]])

#_(defn header [pad]
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
       {:display "inline-block"
        :text-decoration "none"
        :background "#FFF"
        :width "2rem"
        :height "2rem"
        :text-align "center"
        :line-height "2rem"
        :border-radius "50%"
        :box-shadow [[0 (px 1) (px 4) 0 "#ccc"]]
        :color "#AAA"
        :transition [["all" "0.2s" "ease-in-out"]]
        :position "relative"
        :bottom "0"}]

      [:&:hover
       [:.title
        {:box-shadow [[0 (px 4) (px 4) 0 "#ccc"]]
         :bottom "1px"}]]

      [:&.active
       [:.title
        {:background "#AAA"
         :color "#fff"
         :-webkit-font-smoothing "antialiased"}]]

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

     [:.users
      [:.title:after
       (mixins/fontawesome \uf0c0)
       {:margin-left (em 0.25)}]]

     [:.tags
      [:.title:after
       (mixins/fontawesome \uf02c)]]

     [:.settings
      [:.title:after
       (mixins/fontawesome \uf013)]]

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
