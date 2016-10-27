(ns braid.client.register.styles
  (:require
    [braid.client.ui.styles.animations :as animations]
    [braid.client.ui.styles.mixins :as mixins]
    [braid.client.ui.styles.icons :as icons]))

(def small-spacing "0.5rem")
(def border-radius "3px")

(def accent-color          "#2bb8ba")
(def page-background-color "#f3f3f3")
(def form-background-color "#ffffff")
(def primary-text-color    "#222222")
(def secondary-text-color  "#999999")
(def field-border-color    "#e0e0e0")
(def invalid-color         "#fd4734")
(def valid-color           "#2bb8ba")
(def disabled-input-color  "#f6f6f6")
(def placeholder-color     "#eeeeee")
(def disabled-button-color "#cccccc")

(defn input-field-mixin []
  {:font-size "1.25rem"
   :font-family "Open Sans"
   :padding "0.4em"
   :border [["1px" "solid" field-border-color]]
   :line-height 1.5
   :border-radius border-radius})

(def anim-spin animations/anim-spin)

(defn app-styles []
  [:html
   {:background page-background-color
    :border [["1px" "solid" field-border-color]]
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
   {:display "flex"
    :flex-direction "column"
    :height "100vh"
    :max-height "80em"
    :padding [["1.5rem" 0]]
    :box-sizing "border-box"
    :justify-content "space-around"}

   [:h1
    {:margin 0
     :color accent-color
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

   [:.option
    {:margin [[small-spacing 0]]
     :position "relative"}

    [:h2
     {:font-size "1em"
      :color primary-text-color
      :margin [[0 0 small-spacing 0]]}]

    [:.explanation
     {:color secondary-text-color
      :font-size "0.75em"
      :margin [[small-spacing 0 0 0]]}

     [:p
      {:margin "0"}]]

    [:.error-message
     {:position "absolute"
      :top 0
      :right 0
      :text-align "right"
      :line-height "1.5rem"
      :font-size "0.75em"
      :color invalid-color}]

    [:label
     {:display "block"}

     ["input[type=text]"
      "input[type=email]"
      (input-field-mixin)

      [:&:focus
       {:border-color accent-color
        :outline "none"}]]

     ["::-webkit-input-placeholder"
      {:color placeholder-color}]

     ["&&::-moz-placeholder"
      {:color placeholder-color}]]

    [:&.invalid
     ["input[type=text]"
      "input[type=email]"
      {:border-color invalid-color}]

     [:.field::after
      (input-field-mixin)
      {:content "\"\u26a0\""
       :color invalid-color
       :border "none"
       :display "inline-block"}]]

    [:&.valid
     [:.field::after
      (input-field-mixin)
      {:content "\"\u2713\""
       :color valid-color
       :border "none"
       :display "inline-block"}]]

    [:&.loading
     [:.field::after
      (input-field-mixin)
      {:content "\"\u2026\""
       :color secondary-text-color
       :border "none"
       :display "inline-block"}]]

    [:&.email
     :&.group-name
     [:input
      {:width "15em"}]]

    [:&.group-url

     [:.field
      {:white-space "nowrap"}

      ["input[type=text]"
       {:text-align "right"
        :border-radius [[border-radius 0 0 border-radius]]
        :width "8.75em"
        :vertical-align "top" }]

      [:span
       (input-field-mixin)
       {:border-left "none"
        :display "inline-block"
        :vertical-align "top"
        :background disabled-input-color
        :color secondary-text-color
        :border-radius [[0 border-radius border-radius 0]]}

       [:&::after
        {:content "\"\""
         :width "0.15em"
         :display "inline-block"}]]]]

    [:&.group-type

     [:label
      {:margin [[small-spacing 0]]
       :border [["1px" "solid" field-border-color]]
       :padding [["0.75rem" "1rem" "1.0rem"]]
       :border-radius border-radius
       :position "relative"
       :cursor "pointer"}

      [:&.checked
       [:&::after
        (input-field-mixin)
        {:content "\"\u2713\""
         :color valid-color
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
       {:border-color invalid-color}]

      [:.error-message::after
       (input-field-mixin)
       {:content "\"\u26a0\""
        :color invalid-color
        :position "absolute"
        :right "-2em"
        :top "50%"
        :margin-top "-1em"
        :border "none"
        :display "inline-block"}]]]]

   [:button
    {:font-size "1.25em"
     :padding "1rem"
     :background accent-color
     :border "none"
     :color "white"
     :border-radius border-radius
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
     {:background disabled-button-color
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

