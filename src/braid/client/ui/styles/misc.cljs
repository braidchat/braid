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
       :box-sizing "border-box"}] ]])

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


(defn threads [pad]
  [:.page
   ["> .threads"
    mixins/flex
    {:position "absolute"
     :top vars/pad
     :right 0
     :bottom 0
     :left 0
     :padding-left vars/pad

     :align-items "flex-end"
     :overflow-x "scroll" }]])

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
