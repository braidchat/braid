(ns braid.client.register.styles.vars)

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
