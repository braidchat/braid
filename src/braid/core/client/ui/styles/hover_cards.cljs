(ns braid.core.client.ui.styles.hover-cards
  (:require
    [garden.arithmetic :as m]
    [garden.units :refer [rem em px]]
    [braid.core.client.ui.styles.mixins :as mixins]
    [braid.core.client.ui.styles.vars :as vars]))

(def offset (px 5))
(def avatar-size (rem 1.0))

(defn card []
  [:&
   (mixins/box-shadow)

   {:background "white"
    :min-width (em 17)
    :margin-top (m/* -1 offset)
    :margin-left (m/* -1 offset)
    :padding offset
    :border-radius (px 3)
    :overflow "hidden"}

   [:>.header
    {:padding offset
     :margin [[(m/* -1 offset) (m/* -1 offset) 0 (m/* -1 offset)]]
     :white-space "nowrap"
     :color "white"}

    [:>.pill
     mixins/pill-box]]

   [:>.info
    {:font-size (em 0.9)
     :color vars/grey-text
     :margin [[offset 0]]}

    [:>.description
     {:margin [[offset 0]]}]]

   [:>.actions
    [:>a
     :>button
     {:margin-bottom (px 2)
      :margin-right (em 0.5)}
     mixins/pill-button]]])

(def >tag-card
  [:>.card.tag
   (card)

   [:>.header

    [:>.count
     {:margin-left (em 1)
      :display "inline-block"}
     (mixins/mini-text)

     [:&::after
      {:margin-left (em 0.25)
       :-webkit-font-smoothing "antialiased"}]

     [:&.threads

      [:&::after
       (mixins/fontawesome \uf181)]]

     [:&.subscribers

      [:&::after
       (mixins/fontawesome \uf0c0)]]]]])

(def >user-card
  [:>.card.user
   (card)

   [:>.header
    ; reserve room for avatar, which is absolutely positioned
    {:padding-right avatar-size
     :position "relative"}

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
      :position "absolute"
      :top offset
      :right offset}]]

   [:>.local-time

    [:&::after
     (mixins/fontawesome \uf017)
     {:margin-left (em 0.25)}]]])
