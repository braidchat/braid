(ns braid.client.register.styles
  (:require
    [braid.client.ui.styles.fontawesome :as fontawesome]
    [braid.client.ui.styles.animations :as animations]
    [braid.client.ui.styles.mixins :as mixins]
    [braid.client.register.styles-vars :as vars]))

(def anim-spin animations/anim-spin)

(defn app-styles []
  [:body
   {:min-height "100vh"
    :font-family vars/font-family
    :margin 0
    :line-height 1.5
    :background vars/page-background-color
    :color vars/primary-text-color}

    [:input
     :button
     {:font-family vars/font-family}]])

(defn form-styles []
  [:.register

   [:h1.header
    {:color vars/accent-color
     :font-weight "normal"
     :font-size "1.75em"
     :text-align "center"
     :margin "2rem 0"}

    [:&:before
     {:content "\"\""
      :display "inline-block"
      :margin-right "0.5em"
      :margin-bottom "-0.15em"
      :margin-left "-1.75em"
      :width "1.25em"
      :height "1.25em"
      :background-image "url(/images/braid-logo-color.svg)"
      :background-size "contain"}]]

   [:.section
    {:background vars/form-background-color
     :border [["1px" "solid" vars/field-border-color]]
     :max-width "32em"
     :padding "2rem"
     :margin "0 auto"
     :box-sizing "border-box"
     :margin-bottom "2rem"
     :border-radius vars/border-radius}

    [:h1
     {:margin [[0 0 "1.5rem"]]
      :color vars/accent-color
      :font-size "1.2em"}]]

   [:.option
    {:margin [[0 0 "2rem"]]
     :position "relative"}

    [:h2
     {:font-size "1em"
      :color vars/primary-text-color
      :margin [[0 0 vars/small-spacing 0]]}]

    [:.explanation
     (vars/small-text-mixin)]

    [:.error-message
     {:position "absolute"
      :top 0
      :right 0
      :text-align "right"
      :line-height "1.5rem"
      :font-size "0.75em"
      :color vars/invalid-color}]

    [:label
     {:display "block"}

     ["input[type=text]"
      "input[type=email]"
      "input[type=password]"
      (vars/input-field-mixin)

      [:&:focus
       {:border-color vars/accent-color
        :outline "none"}]]

     ["::-webkit-input-placeholder"
      {:color vars/placeholder-color}]

     ["&&::-moz-placeholder"
      {:color vars/placeholder-color}]]

    [:.field
     {:position "relative"}

     [:&::after
      (vars/input-field-mixin)
      ; cancel out some input-field-mixin styles
      {:width "inherit"
       :border "none"
       :margin "1px"}
      {:position "absolute"
       :top 0
       :right "0.25em"}]]

    [:&.invalid
     ["input[type=text]"
      "input[type=email]"
      {:border-color vars/invalid-color}]

     [:.field
      [:&::after
       (fontawesome/mixin :warning)
       {:color vars/invalid-color}]]]

    [:&.valid
     [:.field::after
      (fontawesome/mixin :check-circle)
      {:color vars/valid-color}]]

    [:&.loading
     [:.field::after
      (fontawesome/mixin :spinner)
      mixins/spin
      {:color vars/secondary-text-color}]]]

   [:button.submit
    {:font-size "1.25em"
     :width "100%"
     :padding "1rem"
     :background vars/accent-color
     :border "none"
     :color "white"
     :border-radius vars/border-radius
     :text-transform "uppercase"
     :white-space "nowrap"
     :letter-spacing "0.05em"
     :transition "background 0.2s ease-in-out"
     :font-weight "bold"
     :cursor "pointer"
     :text-align "left"
     :-webkit-font-smoothing "antialiased"
     :display "flex"
     :justify-content "space-between"
     :align-items "center"}

    [:&::after
     (fontawesome/mixin :chevron-right)]

    [:&:hover
     {:background "#2e9394"}]

    [:&:active
     {:background "#185556"}]

    [:&:focus
     {:outline "none"}]

    [:&.disabled
     {:background vars/disabled-button-color
      :cursor "not-allowed"}]

    [:&.sending

     [:&::after
      mixins/spin
      (fontawesome/mixin :spinner)
      {:display "inline-block"}]]]])

