(ns braid.client.register.styles
  (:require
    [braid.client.ui.styles.animations :as animations]
    [braid.client.ui.styles.mixins :as mixins]
    [braid.client.ui.styles.icons :as icons]
    [braid.client.register.styles.vars :as vars]))


(def anim-spin animations/anim-spin)

(defn app-styles []
  [:html
   {:background vars/page-background-color
    :border [["1px" "solid" vars/field-border-color]]
    :border-bottom "none"
    :border-top "none"}

   [:body
    {:min-height "100vh"
     :font-family "Open Sans"
     :max-width "23em"
     :margin "0 auto"
     :line-height 1.5
     :background "white"
     :padding [[0 "5rem"]]}]])

(defn form-styles []
  [:form.register
   {; temporary disable flex styles
    ; during refactor
    ;:display "flex"
    ;:flex-direction "column"
    ;:height "100vh"
    ;:justify-content "space-around"
    :max-height "80em"
    :padding [["1.5rem" 0]]
    :box-sizing "border-box"
    }

   [:h1
    {:margin 0
     :color vars/accent-color
     :font-weight "normal"
     :font-size "1.75em"
     :margin-bottom "0.75rem"}

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

   [:h2
    {:margin [["3rem" 0 "1.5rem"]]}]

   [:.option
    {:margin [[0 0 "2rem"]]
     :position "relative"}

    [:h2
     {:font-size "1em"
      :color vars/primary-text-color
      :margin [[0 0 vars/small-spacing 0]]}]

    [:.explanation
     {:color vars/secondary-text-color
      :font-size "0.75em"
      :margin [[vars/small-spacing 0 0 0]]}

     [:p
      {:margin "0"}]]

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

    [:&.invalid
     ["input[type=text]"
      "input[type=email]"
      {:border-color vars/invalid-color}]

     [:.field::after
      (vars/input-field-mixin)
      {:content "\"\u26a0\""
       :color vars/invalid-color
       :border "none"
       :display "inline-block"}]]

    [:&.valid
     [:.field::after
      (vars/input-field-mixin)
      {:content "\"\u2713\""
       :color vars/valid-color
       :border "none"
       :display "inline-block"}]]

    [:&.loading
     [:.field::after
      (vars/input-field-mixin)
      {:content "\"\u2026\""
       :color vars/secondary-text-color
       :border "none"
       :display "inline-block"}]]]

   [:button
    {:font-size "1.25em"
     :padding "1rem"
     :background vars/accent-color
     :border "none"
     :color "white"
     :border-radius vars/border-radius
     :text-transform "uppercase"
     :white-space "nowrap"
     :letter-spacing "0.05em"
     :display "inline-block"
     :transition "background 0.25s ease-in-out"
     :cursor "pointer"
     :-webkit-font-smoothing "antialiased"}

    [:&::after
     {:content "\"≫\""
      :display "inline-block"
      :width "1.2em"
      :height "1.2em"
      :margin-left "0.25rem"}]

    [:&:focus
     {:outline "none"}]

    [:&.disabled
     {:background vars/disabled-button-color
      :cursor "not-allowed"}]

    [:&.sending

     [:&::after
      mixins/spin
      {:content "\"\""
       :width "1.2em"
       :height "1.2em"
       :display "inline-block"
       :vertical-align "top"
       :margin-left "0.25rem"
       :background-image (icons/spinner "#fff")
       :background-size "contain"}]]]])

