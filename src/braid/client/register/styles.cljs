(ns braid.client.register.styles)

(def braid-color "#2bb8ba")

(def small-spacing "0.5rem")
(def border-radius "3px")

(def invalid-color "#fd4734")
(def valid-color "#2bb8ba")

(defn input-field-mixin []
  {:font-size "1.25rem"
   :font-family "Open Sans"
   :padding "0.4em"
   :border "1px solid #ddd"
   :line-height 1.5
   :border-radius border-radius})

(defn app-styles []
  [:html
   {:background "#f3f3f3"}

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
    :min-height "100vh"
    :padding [["1.5rem" 0]]
    :box-sizing "border-box"
    :justify-content "space-around"}

   [:h1
    {:margin 0
     :color braid-color
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
      :margin [[0 0 small-spacing 0]]}]

    [:.explanation
     {:color "#999"
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
      (input-field-mixin)

      [:&:focus
       {:border-color braid-color
        :outline "none"}]]

     ["::-webkit-input-placeholder"
      {:color "#eee"}]]

    [:&.invalid
     ["input[type=text]"
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

    [:&.group-url

     [:.field
      {:white-space "nowrap"}

      ["input[type=text]"
       {:text-align "right"
        :border-radius [[border-radius 0 0 border-radius]]
        :width "7.5em"
        :vertical-align "top" }]

      [:span
       (input-field-mixin)
       {:border-left "none"
        :display "inline-block"
        :vertical-align "top"
        :background "#f6f6f6"
        :color "#999"
        :border-radius [[0 border-radius border-radius 0]]}

       [:&::after
        {:content "\"\""
         :width "0.15em"
         :display "inline-block"}]]]]

    [:&.group-type

     [:label
      {:margin [[small-spacing 0]]
       :border "1px solid #eee"
       :padding [["0.75rem" "1rem" "1.0rem"]]
       :border-radius border-radius
       :position "relative"}

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
         :margin-right "0.25em"}]]]]]

   [:button
    {:font-size "1.25em"
     :padding "1rem"
     :background braid-color
     :border "none"
     :color "white"
     :border-radius border-radius
     :text-transform "uppercase"
     :white-space "nowrap"
     :letter-spacing "0.05em"
     :display "inline-block"
     :transition "background 0.25s ease-in-out"
     :cursor "pointer"}

    ["&[disabled]"
     {:background "#ccc"
      :cursor "not-allowed"}]

    [:&::after
     {:content "\" ▶\""}]

    [:&:focus
     {:outline "none"}]]])

