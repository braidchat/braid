(ns braid.client.ui.styles.login
  (:require [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def mixin-button-white
  [:&
   {:background "white"
    :border-radius (em 0.5)
    :border "none"
    :color vars/dark-bg-color
    :padding [[(em 1) (em 1.5)]]
    :outline "none"
    :cursor "pointer"}

   [:&:hover
    {:background "rgba(255,255,255,0.8)"}]

   [:&:active
    {:background "rgba(255,255,255,0.6)"}]])

(def login
  [:.login
   mixins/flex
   {:justify-content "center"
    :align-items "center"
    :height "100vh"
    :background vars/dark-bg-color
    :color "white"}

   [:&:before
    {:content "\"\""
     :background-image "url(/images/braid.svg)"
     :background-size "contain"
     :background-repeat "no-repeat"
     :height (rem 17)
     :width (rem 17)
     :background-position "center right"
     :margin-right (rem 3)}]

   [:form
    [:fieldset
     {:margin 0
      :padding 0
      :border "none"}

     [:label
      {:display "block"
       :font-size (em 1)}

      [:input
       {:display "block"
        :width (em 30)
        :border "none"
        :margin [[(em 0.5) 0 (em 1)]]
        :padding (em 1)
        :border-radius (em 0.5)
        :font-size (em 1.5)
        :box-sizing "border-box"
        :outline "none"}]] ]

    [:button.submit
     mixin-button-white
     {:font-size (em 1.5)
      :display "inline-block"
      :margin-top (em 1)
      :margin-right (em 1)}

     [:&:after
      (mixins/fontawesome \uf138)
      {:margin-left (em 0.5)}]]

    [:.spinner
     {:display "inline-block"}
     [:&:before
      (mixins/fontawesome \uf110)
      mixins/spin
      {:font-size (em 2)}]]

    [:.error
     {:display "inline-block"
      :margin-top (em 1)
      :vertical-align "bottom"}

     [:.message
      {:margin [[0 0 (em 0.5)]]}]

     [:button.reset-password
      mixin-button-white
      {:font-size (em 1)}

      [:&:after
       (mixins/fontawesome \uf1d8)
       {:margin-left (em 0.5)}]]]]

   [:.alternatives
    {:margin-top (em 1)}
    [:.github
     [:button
      mixin-button-white
      [:&:before
       (mixins/fontawesome \uf09b)
       {:font-size (em 3)}]]]]])
