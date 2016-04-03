(ns braid.ui.styles.misc
  (:require [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]
            [braid.ui.styles.mixins :as mixins]
            [braid.ui.styles.vars :as vars]))

(def layout
  [:.app
    [:.main

     [:.sidebar
      {:position "absolute"
       :left 0
       :top 0
       :bottom 0
       :width vars/sidebar-width
       :box-sizing "border-box"}]

     [:.page
      {:position "absolute"
       :left vars/sidebar-width
       :top 0
       :bottom 0
       :right 0}]]])

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
    :vertical-align "middle"}])

(def error-banners
  [:.error-banners
   {:z-index 9999
    :position "fixed"
    :top 0
    :right 0
    :width "100%"}

   [:.error-banner
    {:margin-bottom (rem 0.25)
     :font-size (em 2)
     :background-color "rgba(255, 5, 14, 0.6)"
     :text-align "center"}

     [:.close
      {:margin-left (em 1)
       :cursor "pointer"}]]])

(def page
  [:.page

   ["> .title"
    {:height vars/top-bar-height
     :line-height vars/top-bar-height
     :color vars/grey-text
     :margin vars/pad}]

   ["> .content"
    {:overflow "scroll"
     :padding vars/pad
     :color vars/grey-text}

    [:.description
     {:width vars/card-width}

     [:.avatar
      {:width (rem 4)
       :height (rem 4)
       :display "block"
       :border-radius (rem 1)
       :margin-bottom vars/pad}]]]])

(def channels-page
  [:.page.channels

   [:.tags
    {:margin-top (em 1)
     :color "grey-text"}
    [:.tag-info
     {:margin-bottom (em 1)}]]

    [:.count
     {:margin-right (em 0.5)}
      [:&:after
       {:font-family "fontawesome"
        :margin-left (em 0.25)}]

      [:&.threads-count
       [:&:after
        (mixins/fontawesome \uf181)]]

      [:&.subscribers-count
       [:&:after
        (mixins/fontawesome \uf0c0)]]]])

(def me-page
  [:.page.me

   [:.nickname

    [:.error
     {:color "red"}]]

   [:.pending-invites
    {:color "#999"}

    [:ul.invites
     {:padding-left (px 15)}]]

   [:.group

    [:.name
     {:color "#999"
      :margin-bottom (em 0.25)}]

    [:.invite-form

     [:autocomplete
      {:background-color "#999"}

      [:.results
       {:list-style-type "none"
        :padding-left (px 10)}

       [:.result

        [:&:hover
         {:background "#eee"}]]

       [:.active
        {:font-weight "bold"}]]]]

    [:.new-tag.error
     {:border-color "red"}]]])

(def login
  [:.login
   mixins/floating-box

   [:input
    {:width "100%"
     :margin [[0 0 (em 1)]]
     :padding (em 0.25)
     :box-sizing "border-box"}]])

(def tag
  [:.tag
   mixins/pill-box])

(def user
  [:.user
   mixins/pill-box])

(def button
  [:.button
   mixins/pill-box
   {:color vars/grey-text
    :border [[(px 1 "solid" vars/grey-text)]]
    :background "none"
    :margin-left (em 1)}

   [:&:hover
    {:color "#eee"
     :background vars/grey-text
     :cursor "pointer"}]

   [:&:active
    {:color "#eee"
     :background "#666"
     :border-color "#666"
     :cursor "pointer"}]])

(defn threads [pad]
  [:.threads
   mixins/flex
   {:position "absolute"
    :top (+ vars/top-bar-height vars/pad vars/pad)
    :right 0
    :bottom vars/pad
    :left 0
    :padding-left vars/pad

    :align-items "flex-end"
    :overflow-x "scroll" }])
