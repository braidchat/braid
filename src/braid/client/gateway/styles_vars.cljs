(ns braid.client.gateway.styles-vars
  (:require
    [braid.client.ui.styles.fontawesome :as fontawesome]))

(def small-spacing "0.5rem")
(def border-radius "3px")
(def font-family "Open Sans")

(def accent-color          "#2bb8ba")
(def page-background-color "#f3f3f3")
(def form-background-color "#ffffff")
(def primary-text-color    "#444444")
(def secondary-text-color  "#999999")
(def field-border-color    "#e0e0e0")
(def invalid-color         "#fd4734")
(def invalid-light-color   "#ff8477")
(def valid-color           "#2bb8ba")
(def disabled-input-color  "#f6f6f6")
(def placeholder-color     "#eeeeee")
(def disabled-button-color "#cccccc")

(defn input-field-mixin []
  {:font-size "1.25rem"
   :font-family font-family
   :padding "0 0.4em"
   :width "100%"
   :box-sizing "border-box"
   :border [["1px" "solid" field-border-color]]
   :line-height "2em"
   :height "2em"
   :border-radius border-radius})

(defn small-text-mixin []
  [:&
   {:color secondary-text-color
    :font-size "0.75em"
    :margin [[small-spacing 0 0 0]]}
   [:p
    {:margin "0"}]])

(defn small-button-mixin []
  [:&
   {:display "inline-block"
    :border-radius border-radius
    :padding [[0 "0.5em"]]
    :margin-left "0.5em"
    :background "none"
    :color secondary-text-color
    :cursor "pointer"
    :font-size "0.8em"
    :height "1.25rem"
    :line-height "1.25rem"
    :letter-spacing "0.02em"
    :text-transform "uppercase"
    :box-sizing "border-box"
    :border [["1px" "solid" field-border-color]]
    :transition ["border-color 0.25s ease-in-out"
                 "color 0.25s ease-in-out"]}

   [:&::after
    (fontawesome/mixin :angle-double-right)
    {:margin-left "0.25em"}]

   [:&:hover
    {:color "#666666"
     :border-color "#999999"}]

   [:&:active
    {:background secondary-text-color}]])

(defn padded-with-border-mixin []
  {:padding "1em"
   :border [["1px" "solid" field-border-color]]
   :border-radius border-radius})
