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

     [:.right

      [".bar:hover + .options"
       ".options:hover"
       {:display "inline-block"}]

      [:.bar
       {:position "absolute"
        :right vars/pad
        :top vars/pad
        :z-index 100
        :background "black"
        :color "white"
        :height header-height
        :border-radius vars/border-radius
        :overflow "hidden"}
       (mixins/box-shadow)

       [:.user-info
        {:display "inline-block"
         :vertical-align "top"}

        [:&:hover
         :&.active
         {:background "#666"}]

        [:.name
         {:color "white"
          :padding [[0 (m/* vars/pad 0.75)]]
          :text-transform "uppercase"
          :letter-spacing "0.1em"
          :display "inline-block"
          :text-decoration "none"
          :vertical-align "top"
          :font-weight "bold"
          :line-height header-height
          :-webkit-font-smoothing "antialiased"}]

        [:.avatar
         {:height header-height
          :width header-height}]]

       [:.more
        {:display "inline-block"
         :line-height header-height
         :vertical-align "top"
         :height header-height
         :width header-height
         :text-align "center"}

        [:&:after
         {:-webkit-font-smoothing "antialiased"}
         (mixins/fontawesome \uf078)]]]

      [:.options
       {:background "white"
        :padding [[(m/* vars/pad 0.75)]]
        :border-radius vars/border-radius
        :position "absolute"
        :top vars/pad
        :right vars/pad
        :margin-top header-height
        :z-index 110
        :display "none"}
       (mixins/box-shadow)

       ; little arrow above options box
       [:&:before
        {:position "absolute"
         :top "-0.65em"
         :right (m/* vars/pad 0.70)
         :color "white"
         :font-size "1.5em"}
         (mixins/fontawesome \uf0d8)]

        [:a
         {:display "block"
          :color "black"
          :text-align "right"
          :text-decoration "none"
          :line-height "1.85em"}

          [:&:hover
           {:color "#666"}]

          [:&:after
           {:margin-left "0.5em"}]

          [:&.subscriptions:after
           (mixins/fontawesome \uf02c)]

          [:&.invite-friend:after
           (mixins/fontawesome \uf1e0)]

          [:&.edit-profile:after
           (mixins/fontawesome \uf007)]

          [:&.notification-settings:after
           (mixins/fontawesome \uf013)]]]]]

      [:.left
       {:position "absolute"
        :left vars/sidebar-width
        :top vars/pad
        :z-index 100
        :margin-left vars/pad
        :border-radius vars/border-radius
        :overflow "hidden"
        :height header-height}
       (mixins/box-shadow)

       [:.group-name
        :a
        {:color "white"
         :background "black"
         :display "inline-block"
         :vertical-align "top"
         :height header-height
         :line-height header-height
         :-webkit-font-smoothing "antialiased"}]

       [:.group-name
        {:text-transform "uppercase"
         :padding-right (m/* vars/pad 0.25)
         :padding-left (m/* vars/pad 0.75)
         :letter-spacing "0.1em"
         :font-weight "bold"}]

       [:a
        {:padding [[0 (m/* vars/pad 0.5)]]
         :text-decoration "none"}

        [:&:hover
         :&.active
         {:background "#666"}]

        [:&.inbox:after
         (mixins/fontawesome \uf01c)]

        [:&.recent:after
         (mixins/fontawesome \uf1da)]]

       [:.search-bar
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
