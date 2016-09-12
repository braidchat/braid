(ns braid.client.mobile.style
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-import]]
            [garden.arithmetic :as m]
            [garden.units :refer [rem vw vh em]]
            [braid.client.mobile.auth-flow.styles]
            [braid.client.mobile.styles.drawer]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.header]
            [braid.client.ui.styles.imports]
            [braid.client.ui.styles.pills]
            [braid.client.ui.styles.thread]
            [braid.client.ui.styles.body]
            [braid.client.ui.styles.message]
            [braid.client.ui.styles.embed]
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

         (braid.client.mobile.auth-flow.styles/auth-flow)

         (braid.client.mobile.styles.drawer/drawer pad)

         [:body
          {:touch-action "none"}]

         [:.page
          {:position "absolute"
           :top 0
           :left 0
           :right 0
           :bottom 0
           :z-index 50
           :background "#CCC" }]

         (braid.client.ui.styles.header/group-header "2.5rem")

         [:.group-header
          mixins/flex
          {:justify-content "space-between"}

          [:.group-name
           {:padding 0}]

          [:.spacer
           {:flex-grow 2}]

          [:.buttons]]

         [:.threads
          {:padding-top "2.5rem"
           :margin-top "-2.5rem"
           :height "100%"
           :box-sizing "border-box"}

          [:.thread
           mixins/flex
           {:width "100vw"
            :height "100%"
            :background "white"
            :flex-direction "column"}

           (braid.client.ui.styles.thread/head pad)

           [:.head
            [:.tags
             (braid.client.ui.styles.pills/tag)
             (braid.client.ui.styles.pills/user)]

            [:.close
             {:position "absolute"
              :top 0
              :right 0
              :color "#CCC"
              :padding vars/pad}

             [:&:after
              (mixins/fontawesome \uf00d)]]]

           (braid.client.ui.styles.thread/messages pad)

           [:.messages
            {:flex-grow 1}

           (braid.client.ui.styles.embed/embed pad)]

           (braid.client.ui.styles.thread/new-message pad)]]

         braid.client.ui.styles.body/body

         braid.client.ui.styles.message/message)))
