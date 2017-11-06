(ns braid.client.mobile.style
  (:require
    [braid.client.mobile.auth-flow.styles]
    [braid.client.mobile.styles.drawer]
    [braid.client.ui.styles.body]
    [braid.client.ui.styles.embed]
    [braid.client.ui.styles.header]
    [braid.client.ui.styles.imports]
    [braid.client.ui.styles.message]
    [braid.client.ui.styles.mixins :as mixins]
    [braid.client.ui.styles.pills]
    [braid.client.ui.styles.thread]
    [braid.client.ui.styles.vars :as vars]
    [garden.arithmetic :as m]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]
    [garden.units :refer [rem vw vh em]]))

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
          {:touch-action "none"}

          [:>#app

           [:>.app
            braid.client.mobile.auth-flow.styles/auth-flow

            [:>.main
             (braid.client.mobile.styles.drawer/drawer pad)

             [:>.page
              {:position "absolute"
               :top 0
               :left 0
               :right 0
               :bottom 0
               :z-index 50
               :background "#CCC"}

              (braid.client.ui.styles.header/group-header "2.5rem")

              [:>.global-settings
               {:overflow-y "auto"
                :height "90vh"
                :margin-left "5em"}]

              [:>.group-header
               [:>.bar
                mixins/flex
                {:justify-content "space-between"}

                [:>.group-name
                 {:padding 0}]

                [:>.badge-wrapper
                 {:min-width (em 1.5)}
                 [:>.badge
                  mixins/pill-box
                  {:background-color "#b53737 !important"
                   :margin-left (em 0.5)
                   :margin-top (em 0.5)}]]

                [:>.spacer
                 {:flex-grow 2}]]]

              [:>.threads
               {:padding-top "2.5rem"
                :margin-top "-2.5rem"
                :height "100%"
                :box-sizing "border-box"}

               [:>.container

                [:>.panels

                 [:>.panel
                  {:position "relative"}

                  [:>.arrow-prev
                   :>.arrow-next
                   {:position "absolute"
                    :bottom "50%"}]

                  [:>.arrow-prev
                   [:&::before
                    (mixins/fontawesome \uf104)
                    {:font-size (em 3)
                     :color "lightgray"}]
                   {:left 0}]

                  [:>.arrow-next
                   [:&::after
                    (mixins/fontawesome \uf105)
                    {:font-size (em 3)
                     :color "lightgray"}]
                   {:right 0}]

                  [:>.thread
                   mixins/flex
                   {:width "100vw"
                    :height "100%"
                    :background "white"
                    :flex-direction "column"
                    :justify-content "flex-end"}

                   [:>.card
                    (braid.client.ui.styles.thread/head pad)

                    [:>.head

                     [:>.tags
                      (braid.client.ui.styles.pills/tag)
                      (braid.client.ui.styles.pills/user)]

                     [:>.close
                      {:position "absolute"
                       :top 0
                       :right 0
                       :color "#CCC"
                       :padding vars/pad}

                      [:&::after
                       (mixins/fontawesome \uf00d)]]]

                    (braid.client.ui.styles.thread/messages pad)

                    [:>.messages
                     {:flex-grow 1
                      :max-height "75vh"}

                     braid.client.ui.styles.message/message
                     [:>.message
                      (braid.client.ui.styles.embed/embed pad)]]

                    (braid.client.ui.styles.thread/new-message pad)
                    ]]]]]]]]]]]

         braid.client.ui.styles.body/body)))
