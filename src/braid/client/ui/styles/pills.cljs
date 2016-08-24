(ns braid.client.ui.styles.pills
  (:require [garden.units :refer [rem em px]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]
            [garden.arithmetic :as m]))

(def offset (px 5))
(def avatar-size (rem 2.5))

(defn hover-card []
  [:&
   {:position "relative"
    :display "inline-block"}

   [:&:hover
    [:.card
     {:display "block"}]]

   [:.pill
    mixins/pill-box]

   [:.card
    (mixins/box-shadow)
    {:display "none"
     :position "absolute"
     :z-index 5000
     :background "white"
     :min-width (em 15)
     :top (m/* -1 offset)
     :left (m/* -1 offset)
     :padding offset
     :border-radius (px 3)
     :overflow "hidden"}

    [:.header
     {:padding offset
      :margin [[(m/* -1 offset) (m/* -1 offset) 0 (m/* -1 offset)]]
      :white-space "nowrap"
      :color "white"}]

    [:.info
     {:font-size (em 0.9)
      :color vars/grey-text
      :margin [[offset 0]]}

     [:.description
      {:margin [[offset 0]]}]]

    [:.actions
     [:a
      {:margin-bottom (px 2)
       :margin-right (em 1)}
      mixins/pill-button]]]])

(def tag
  [:.tag
   (hover-card)

   [:.card

    [:.count
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

(def user
  [:.user
   (hover-card)

   [:.card

    [:.header
     ; reserve room for avatar, which is absolutely positioned
     {:padding-right avatar-size}

     [:.status
      {:display "inline-block"
       :margin-left (em 0.5)}
      (mixins/mini-text)]

     [:.badges
      {:display "inline-block"
       :margin [[0 (em 0.5)]]}
      [:.admin::before
       {:display "inline-block"
        :-webkit-font-smoothing "antialiased"}
       (mixins/fontawesome \uf0e3)]]

     [:img.avatar
      {:margin-top (px 2)
       :border-radius (px 3)
       :width avatar-size
       :height avatar-size
       :background "white"
       :position "absolute"
       :top offset
       :right offset}]]

    [:.local-time

     [:&::after
      (mixins/fontawesome \uf017)
      {:margin-left (em 0.25)}]]]])
