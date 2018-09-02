(ns braid.search.ui.search-page-styles
  (:require
    [garden.units :refer [rem em px]]
    [braid.core.client.ui.styles.mixins :as mixins]
    [braid.core.client.ui.styles.vars :as vars]))

(def avatar-size (rem 4))

(def card-style
  [:>.card
   {:margin-bottom "50%"
    :max-width (rem 25)}

   [:>.header
    {:overflow "auto"}

    [:>.pill.off
     :>.pill.on
     {:color [["white" "!important"]]}]

    [:>.status
     {:display "inline-block"
      :margin-left (em 0.5)}
     (mixins/mini-text)]

    [:>.badges
     {:display "inline-block"
      :margin [[0 (em 0.5)]]}

     [:>.admin::before
      {:display "inline-block"
       :-webkit-font-smoothing "antialiased"}
      (mixins/fontawesome \uf0e3)]]

    [:>img.avatar
     {:margin-top (px 2)
      :border-radius (px 3)
      :width avatar-size
      :height avatar-size
      :background "white"
      :float "left"}]]

   [:>.local-time

    [:&::after
     (mixins/fontawesome \uf017)
     {:margin-left (em 0.25)}]]])

(def >search-page
  [:>.page.search

   [:>.threads
    card-style]

   [:>.content
    card-style]])
