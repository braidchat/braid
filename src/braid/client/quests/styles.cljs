(ns braid.client.quests.styles
  (:require [garden.units :refer [em px rem]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.mixins :as mixins]))

(def quest-icon-size (rem 2))

(defn quests-header [header-text header-height]
  [:&
   [:.quests-header
    {:position "relative"}

    [".bar:hover + .quests-menu"
     ".quests-menu:hover"
     {:display "inline-block"}]

    [:.bar
     (header-text)

     [:&:before
      (mixins/fontawesome \uf091)
      {:margin-right (em 0.5)}]]

    [:.quests-menu
     (mixins/context-menu)
     {:position "absolute"
      :top header-height
      :right 0
      :z-index 150
      :display "none"}

     [:.content

      [:.quest
       {:margin-bottom (em 2)
        :display "flex"
        :justify-content "space-between"}

       [:&:last-child
        {:margin-bottom 0}]

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
          :width (em 18)}]]

       [:.actions
        {:align-self "center"}
        [:a
         (mixins/outline-button)
         {:margin-left (em 0.5)}]]]]]]])
