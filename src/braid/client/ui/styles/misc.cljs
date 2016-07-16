(ns braid.client.ui.styles.misc
  (:require [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def layout
  [:.app
    [:.main

     [:.sidebar
      {:position "fixed"
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
     :text-align "center"}
    [:&.error
     {:background-color "rgba(255, 5, 14, 0.6)"}]
    [:&.warn
     {:background-color "rgba(255, 190, 5, 0.6)"}]
    [:&.info
     {:background-color "rgba(5, 255, 70, 0.6)"}]

     [:.close
      {:margin-left (em 1)
       :cursor "pointer"}]]])

(def page-headers
  [[".page > .title::before"
    {:margin-right (em 0.5)}]

   [".page.inbox > .title::before"
    (mixins/fontawesome \uf01c)]

   [".page.recent > .title::before"
    (mixins/fontawesome \uf1da)]

   [".page.users > .title::before"
    (mixins/fontawesome \uf0c0)]

   [".page.channels > .title::before"
    (mixins/fontawesome \uf02c)]

   [".page.settings > .title::before"
    (mixins/fontawesome \uf013)]

   [".page.search > .title::before"
    (mixins/fontawesome \uf002)]

   [".page.me > .title::before"
    (mixins/fontawesome \uf007)]

   [".page.userpage > .title::before"
    (mixins/fontawesome \uf007)]

   [".page.channel > .title::before"
    (mixins/fontawesome \uf02b)]])

(def page
  [:.page

   ["> .title"
    {:height vars/top-bar-height
     :line-height vars/top-bar-height
     :color vars/grey-text
     :margin vars/pad}]

   ["> .intro"
    {:color vars/grey-text
     :margin vars/pad
     ; make intro above threads, so you can click on clear inbox button
     :z-index 100
     :position "relative"}]

   ["> .content"
    {:overflow "scroll"
     :padding vars/pad
     :color vars/grey-text}

    [:.description
     {:width vars/card-width
      ; we want description to appear above the new thread, but below other threads
      ; TODO: is there a way to make description clickable but not cover
      ; threads that are long enough?
      :z-index 100
      ; opacity & position are hacks to make z-index work
      :position "relative" ; z-index only applies if element has a position set
      :opacity 0.99 ; need to create a new rendering context b/c threads are absolute postioned
      }

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

(def settings-page
  [:.page.settings
   [:.setting
    [:&.avatar
     [:.upload
      [:.uploading-indicator
       {:display "inline-block"}]]]]])

(def me-page
  [:.page.me

   [:.nickname

    [:.error
     {:color "red"}]]

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

(def bots-page
  [:.page.bots
   [:.bots-list
    mixins/flex
    {:flex-direction "row"
     :flex-wrap "wrap"
     :align-content "space-between"
     :align-items "baseline"}
    [:.bot
     {:margin (em 1)}
     [:.avatar
      {:width (rem 4)
       :height (rem 4)
       :display "block"
       :border-radius (rem 1)
       :margin-bottom vars/pad}]]]])

(def uploads-page
  [:.page.uploads
   [:.uploads
    {:width "100%"
     :flex-direction "row"
     :flex-wrap "wrap"
     :align-content "space-between"
     :align-item "bottom"}
    [:.upload
     [:.embed
      {:width (rem 5)
       :margin 0}]
     [:.uploaded-thread
      [:.tags
       {:display "inline"}]]
     {:height (rem 3)
      :margin (em 1)}]]])

(def tag
  [:.tag
   mixins/pill-box])

(def user
  [:.user
   mixins/pill-box
   [:&.admin:before
    (mixins/fontawesome \uf0e3)]])

(def button
  [:.button
   mixins/pill-button
   {:margin-left (em 1)}])

(defn threads [pad]
  [:.threads
   mixins/flex
   {:position "absolute"
    :top (m/+ vars/top-bar-height vars/pad vars/pad)
    :right 0
    :bottom 0
    :left 0
    :padding-left vars/pad

    :align-items "flex-end"
    :overflow-x "scroll" }])

(def status
  [".app > .status"
   mixins/flex
   {:position "absolute"
    :top 0
    :bottom 0
    :right 0
    :left 0
    :background vars/dark-bg-color
    :color "white"
    :justify-content "center"
    :align-items "center"
    :font-size "2em"}
   [:&:before
    (mixins/fontawesome \uf110)
    mixins/spin
    {:display "inline-block"
     :margin-right (em 0.5)}]])
