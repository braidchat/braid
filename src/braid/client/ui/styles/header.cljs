(ns braid.client.ui.styles.header
  (:require [garden.units :refer [em px rem]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.mixins :as mixins]

            [braid.client.quests.styles :refer [quests-header]]))

(def header-height vars/top-bar-height)

(defn header-text [size]
  {:text-transform "uppercase"
   :letter-spacing "0.1em"
   :font-weight "bold"
   :line-height size
   :-webkit-font-smoothing "antialiased"
   :padding [[0 (m/* vars/pad 0.75)]] })

(defn menu []
  [:.options
    (mixins/context-menu)

    {:position "absolute"
     :top 0
     :right 0
     :margin-top header-height
     :z-index 110
     :display "none"}

    [:a
     {:display "block"
      :color "black"
      :text-align "right"
      :text-decoration "none"
      :line-height "1.85em"
      :white-space "nowrap"}

     [:&:hover
      {:color "#666"}]

     [:&:after
      {:margin-left "0.5em"}]

     [:&.subscriptions:after
      (mixins/fontawesome \uf02c)]

     [:&.invite-friend:after
      (mixins/fontawesome \uf1e0)]

     [:&.edit-profile:after
      (mixins/fontawesome \uf007)]

     [:&.group-bots:after
      (mixins/fontawesome \uf12e)]

     [:&.group-uploads:after
      (mixins/fontawesome \uf0ee)]

     [:&.changelog:after
      (mixins/fontawesome \uf1da)]

     [:&.settings:after
      (mixins/fontawesome \uf013)]]])

(defn group-header [size]
  [:.group-header

   [:.bar
    {:display "inline-block"
     :vertical-align "top"}]

   [:.group-name
    :a
    {:color "white"
     :display "inline-block"
     :vertical-align "top"
     :height size
     :line-height size
     :-webkit-font-smoothing "antialiased"}]

   [:.group-name
    (header-text size)
    {:min-width (em 5)}]

   [:.buttons
    {:display "inline-block"
     :vertical-align "top"}]

   [:a
    {:width size
     :text-align "center"
     :text-decoration "none"}

    [:&:hover
     :&.active
     {:background "rgba(0,0,0,0.25)"}]

    [:&.open-sidebar:after
     (mixins/fontawesome \uf0c9)]

    [:&.inbox:after
     (mixins/fontawesome \uf01c)]

    [:&.recent:after
     (mixins/fontawesome \uf1da)]]

   [:.search-bar
    {:display "inline-block"
     :position "relative"}

    [:input
     {:border 0
      :padding-left vars/pad
      :min-width "15em"
      :width "25vw"
      :height size
      :outline "none"}]

    [:.action
     [:&:after
      {:top 0
       :right (m/* vars/pad 0.75)
       :height size
       :line-height size
       :position "absolute"
       :cursor "pointer"}]

     [:&.search:after
      {:color "#ccc"
       :pointer-events "none"}
      (mixins/fontawesome \uf002)]

     [:&.clear:after
      (mixins/fontawesome \uf057)]]]

   [:.loading-indicator
    {:display "inline-block"
     :vertical-align "middle"}

    [:&:before
     {:height size
      :line-height size
      :margin-left (em 0.5)
      :font-size (em 1.5)}]

    [:&.error
     [:&:before
      (mixins/fontawesome\uf071)]]

    [:&.loading
     [:&:before
      (mixins/fontawesome \uf110)
      mixins/spin]]]])

(defn admin-header [size]
  [:.admin-header
   {:position "relative"}

   [:.admin-icon
    (header-text size)
    {:padding [[0 (em 1)]]
     :color "#CCC"
     :margin-left (em 1)}

    [:&:before
     (mixins/fontawesome \uf0e3)]]

   (menu)

   [".admin-icon:hover + .options"
    ".options:hover"
    {:display "inline-block"}]])


(defn user-header [size]
  [:.user-header
   {:position "relative"}

   [".bar:hover + .options"
    ".options:hover"
    {:display "inline-block"}]

   [:.bar
    {:margin-left vars/pad}

    [:.user-info
     {:display "inline-block"
      :vertical-align "top"}

     [:&:hover
      :&.active
      {:background "rgba(0,0,0,0.25)"}]

     [:.name
      (header-text size)
      {:color "white"
       :display "inline-block"
       :text-decoration "none"
       :vertical-align "top" }]

     [:.avatar
      {:height header-height
       :width header-height
       :background "white"}]]

    [:.more
     {:display "inline-block"
      :line-height header-height
      :vertical-align "top"
      :height header-height
      :width header-height
      :text-align "center"}

     [:&:after
      {:-webkit-font-smoothing "antialiased"}
      (mixins/fontawesome \uf078)]]]

   (menu)])

(defn header [pad]
  [:.app
   [:.main
    ["> .header"
     {:position "absolute"
      :top vars/pad
      :left vars/sidebar-width
      :margin-left vars/pad
      :right vars/pad
      :display "flex"
      :justify-content "space-between"
      :z-index 102}

     [:.bar
      {:background "black"
       :color "white"
       :height header-height
       :border-radius vars/border-radius
       :overflow "hidden"}
      (mixins/box-shadow)]

     (group-header header-height)
     [:.spacer
      {:flex-grow 2}]
     (quests-header header-text header-height)
     (user-header header-height)
     (admin-header header-height)]]])
