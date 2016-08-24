(ns braid.client.mobile.style
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-import]]
            [garden.arithmetic :as m]
            [garden.units :refer [rem vw vh em]]
            [braid.client.ui.styles.body]
            [braid.client.ui.styles.message]
            [braid.client.ui.styles.misc]
            [braid.client.ui.styles.imports]
            [braid.client.ui.styles.thread]
            [braid.client.ui.styles.sidebar]
            [braid.client.ui.styles.mixins :as mixins]))

(def styles
  (let [pad (rem 1) ; (vw 5)
        pads "1rem" ; "5vw"
        ]
    (css {:auto-prefix #{:transition
                         :flex-direction
                         :flex-shrink
                         :align-items
                         :animation
                         :flex-grow}
          :vendors ["webkit"]}

         braid.client.ui.styles.imports/imports

         [:body
          {:touch-action "none"}]

         [:.login-flow
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
            {:color "rgba(255,255,255,0.5)"}]]]

         [:.drawer

          [:.content
           {:width "100%"
            :padding pad
            :box-sizing "border-box"}

           (let [icon-w "15vw"]
             [:.group
              {:display "block"
               :height icon-w
               :width icon-w
               :margin-bottom "1rem"
               :position "relative"
               :border-radius "0.5rem"
               }

              [:&.active:before
               (let [w "2vw"]
                 {:content "\"\""
                  :display "block"
                  :width w
                  :background "#eee"
                  :height icon-w
                  :position "absolute"
                  :left (m/- pad)
                  :border-radius [[0 w w 0]]
                  })]

              [:img
               {:width icon-w
                :height icon-w
                :background "white"
                :border-radius "0.5rem"
                :vertical-align "middle"}]

              braid.client.ui.styles.sidebar/badge

              [:.badge
               {:font-size "0.8rem"}]])]]

         ;braid.client.ui.styles.misc/tag

         [:.page
          {:position "absolute"
           :top 0
           :left 0
           :right 0
           :bottom 0
           :overflow "hidden"
           :z-index 50
           :background "#CCC"
           }]


         [:.threads
          [:.thread
           {:width "100vw"
            :height "100vh"
            :background "white"}

           ]]

         (braid.client.ui.styles.thread/head pad)
         (braid.client.ui.styles.thread/messages pad)

         braid.client.ui.styles.body/body
         braid.client.ui.styles.message/message)))
