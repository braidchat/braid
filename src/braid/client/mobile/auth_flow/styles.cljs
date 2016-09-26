(ns braid.client.mobile.auth-flow.styles
  (:require [braid.client.ui.styles.mixins :as mixins]
            [garden.units :refer [rem vw vh em]]))

(defn auth-flow []
  [:.auth-flow
   mixins/flex
   {:background "green"
    :position "absolute"
    :top 0
    :left 0
    :right 0
    :bottom 0
    :overflow "hidden"
    :font-size (em 1.5)
    :color "#fff"
    :justify-content "center"
    :align-items "center"}

   [:.content
    {:max-width (vw 50)
     :flex-grow 1}

    [:&.email :&.password
     {:max-width (vw 65)}]]

   [:.logo
    {:width "100%"
     :height (vw 50)
     ; TODO use flexbox to make this equal to top margin
     :margin-bottom (vw 25)}]

   [:button
    {:font-size (em 1)
     :background "none"
     :border [[(em 0.1) "white" "solid"]]
     :width "100%"
     :box-sizing "border-box"
     :border-radius (em 0.25)
     :color "white"
     :padding (em 0.5)
     :display "block"
     :outline "none"
     :margin-bottom (em 1)
     :transition [["background" "0.1s" "ease-in-out"]]}

    [:&:active
     {:background "rgba(255,255,255,0.25)"}]

    [:&.next:after
     (mixins/fontawesome \uf04b)
     {:margin-left (em 0.5)}]]

   [:input
    {:background "rgba(255,255,255,0.15)"
     :border-top "none"
     :border-right "none"
     :border-left "none"
     :border-bottom [[(rem 0.25) "solid" "#fff"]]
     :padding (em 0.5)
     :font-size (em 1)
     :color "#fff"
     :display "block"
     :width "100%"
     :box-sizing "border-box"
     :margin-bottom (em 1)
     :outline "none"}

    [:&::-webkit-input-placeholder
     {:color "rgba(255,255,255,0.5)"}]]])
