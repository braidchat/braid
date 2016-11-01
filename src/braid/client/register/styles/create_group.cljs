(ns braid.client.register.styles.create-group
  (:require
    [braid.client.register.styles.vars :as vars]))


(defn create-group-styles []

  [:form

   [:.option

    [:&.group-url

     [:.field
      {:white-space "nowrap"}

      ["input[type=text]"
       {:text-align "right"
        :border-radius [[vars/border-radius 0 0 vars/border-radius]]
        :width "8.75em"
        :vertical-align "top" }]

      [:span
       (vars/input-field-mixin)
       {:border-left "none"
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
      {:margin [[vars/small-spacing 0]]
       :border [["1px" "solid" vars/field-border-color]]
       :padding [["0.75rem" "1rem" "1.0rem"]]
       :border-radius vars/border-radius
       :position "relative"
       :cursor "pointer"}

      [:&.checked
       [:&::after
        (vars/input-field-mixin)
        {:content "\"\u2713\""
         :color vars/valid-color
         :position "absolute"
         :right "-2em"
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
        {:display "inline"
         :margin-right "0.25em"}]]]

     [:&.invalid

      [:label
       {:border-color vars/invalid-color}]

      [:.error-message::after
       (vars/input-field-mixin)
       {:content "\"\u26a0\""
        :color vars/invalid-color
        :position "absolute"
        :right "-2em"
        :top "50%"
        :margin-top "-1em"
        :border "none"
        :display "inline-block"}]]]]])
