(ns braid.emoji.styles
  (:require
   [garden.units :refer [rem em px ex]]))

;; Taken from https://github.com/emojione/emojione/blob/2.2.7/assets/css/emojione.css#L1
(def emojione
  [:.emojione
   {:font-size "inherit"
    :height (ex 3)
    :width (ex 3.1)
    :min-height (px 20)
    :min-width (px 20)
    :display "inline-block"
    :margin [[(ex -0.2) (em 0.15) (ex 0.2)]]
    :line-height "normal"
    :vertical-align "middle"}
   ;; Taken from https://github.com/emojione/emojione/blob/2.2.7/assets/css/emojione-awesome.css#L24
   [:&.large
    {:width (em 3)
     :height (em 3)
     :margin [[(ex -0.6) (em 0.15) 0 (em 0.3)]]
     :background-size [[(em 3) (em 3)]]}]])

(def autocomplete
  [:.app>.main>.page>.threads>.thread>.card
   [:>.message.new
    [:>.autocomplete-wrapper
     [:>.autocomplete
      [:>.result
       [:>.match
        [:>.emojione
         {:display "block"
          :width "2em"
          :height "2em"
          :float "left"
          :margin "0.25em 0.5em 0.25em 0"}]]]]]]])
