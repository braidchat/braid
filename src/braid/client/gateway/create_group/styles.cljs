(ns braid.client.gateway.create-group.styles
  (:require
    [braid.client.gateway.styles-vars :as vars]
    [braid.client.ui.styles.fontawesome :as fontawesome]))

(defn create-group-styles []
  [:#app

   [:>.gateway

    [:>.create-group

     [:&.disabled
      {:opacity 0.3
       :cursor "not-allowed"}

      [:>form
       {:pointer-events "none"}]]

     [:>form

      [:>fieldset
       {:margin 0
        :padding 0
        :border "none"}

       [:>.option

        [:&.group-name
         [:input
          {:width "15em"}]]

        [:&.group-url

         [:.field
          {:white-space "nowrap"}

          ["input[type=text]"
           {:text-align "right"
            :border-radius [[vars/border-radius 0 0 vars/border-radius]]
            :width "10em"
            :vertical-align "top" }]

          [:span
           (vars/input-field-mixin)
           {:width "inherit"
            :border-left "none"
            :display "inline-block"
            :vertical-align "top"
            :background vars/disabled-input-color
            :color vars/secondary-text-color
            :border-radius [[0 vars/border-radius vars/border-radius 0]]}

           [:&::after
            {:content "\"\""
             :width "0.15em"
             :display "inline-block"}]]]]

        [:&.group-type

         [:label
          (vars/padded-with-border-mixin)
          {:margin [[vars/small-spacing 0]]
           :padding [["0.75rem" "1rem" "1.0rem"]]
           :position "relative"
           :cursor "pointer"}

          [:&.checked

           [:&::after
            (vars/input-field-mixin)
            (fontawesome/mixin :check-circle)
            {:width "inherit"
             :color vars/valid-color
             :position "absolute"
             :right "0.25em"
             :top "50%"
             :margin-top "-1em"
             :border "none"
             :display "inline-block"}]]

          [:span
           {:display "inline-block"
            :vertical-align "middle"
            :margin-left "0.35rem"}]

          [:.explanation
           {:margin-left "1.5rem"
            :margin-top "0.25em"}

           [:p
            {:margin-right "0.25em"}]]]

         [:&.invalid

          [:label
           {:border-color vars/invalid-color}]

          [:.error-message::before
           (vars/input-field-mixin)
           (fontawesome/mixin :warning)
           {:width "inherit"
            :color vars/invalid-color
            :border "none"
            :vertical-align "middle"}]]]]]]]]])
