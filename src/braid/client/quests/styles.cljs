(ns braid.client.quests.styles
  (:require [garden.units :refer [em px rem]]
            [garden.arithmetic :as m]
            [garden.stylesheet :refer [at-keyframes]]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.mixins :as mixins]))

(def quest-icon-size (rem 2.5))

(defn quests-header [header-text header-height]
  [:&

   [:.quests-header
    {:position "relative"}

    [".quests-icon:hover + .quests-menu"
     ".quests-menu:hover"
     {:display "inline-block"}]

    [:.quests-icon
     (header-text)
     {:padding 0}

     [:&:before
      (mixins/fontawesome \uf091)]

     ; make it easier to hover over the menu
     [:&:hover
      {:padding-left (em 5)}]]

    [:.quests-menu
     (mixins/context-menu)
     {:position "absolute"
      :top header-height
      :right (rem -0.5)
      :z-index 150
      :display "none"}

     [:.content

      [:.congrats
       {:min-width (em 22)}

       [:&::before
        (mixins/fontawesome \uf091)
        {:font-size (em 4.5)
         :float "left"
         :margin-right (rem 0.75)}]

       [:h1
        {:font-size (em 1.2)
         :margin 0}]

        [:p
         {:margin 0}]]

      [:.quests

       [:.quest
        {:margin-bottom (em 2)}

        [:&:last-child
         {:margin-bottom 0}]

        [:.main
         {:display "flex"
          :justify-content "space-between"}

         [:&:before
          {:content "attr(data-icon)"
           :font-family "FontAwesome"
           :display "block"
           :font-size quest-icon-size
           :color "#666"
           :margin [[0 vars/pad 0 (m// vars/pad 2)]]
           :align-self "center"}]

         [:.info
          {:margin-right (em 1)}

          [:h1
           {:font-size (em 1.2)
            :margin 0
            :display "inline-block"}]

          [:.progress
           {:display "inline-block"
            :float "right"}

           [:.icon
            {:display "inline-block"
             :font-size (em 1.2)
             :margin-right (em 0.5)
             :vertical-align "bottom"}

            [:&.incomplete::before
             (mixins/fontawesome \uf10c)]

            [:&.complete::before
             (mixins/fontawesome \uf058)]]]

          [:p
           {:margin 0
            :width (em 20)}]]

         [:.actions
          {:align-self "center"
           :white-space "nowrap"}
          [:a
           (mixins/outline-button)
           {:margin-left (em 0.5)}

           [:&.video
            {:width (em 5.5)}]

           [:&.video::after
            {:margin [[0 (em 0.25)]]}]

           [:&.video.show::after
            (mixins/fontawesome \uf144)]

           [:&.video.hide::after
            (mixins/fontawesome \uf28d)]]]]

        ["> .video"
         {:margin [[(em 1) (em -1) 0]]}
         [:img
          {:width "100%"}]]]]]]]])
