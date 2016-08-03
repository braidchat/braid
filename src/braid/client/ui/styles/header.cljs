(ns braid.client.ui.styles.header
  (:require [garden.units :refer [em px rem]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.mixins :as mixins]))

(def header-height vars/avatar-size)

(defn mixins-context-menu []
  [:&
   {:background "white"
    :border-radius vars/border-radius}
   (mixins/box-shadow)

   [:.content
    {:overflow-x "scroll"
     :height "100%"
     :box-sizing "border-box"
     :padding [[(m/* vars/pad 0.75)]]}]

   ; little arrow above options box
   [:&:before
    (mixins/fontawesome \uf0d8)
    {:position "absolute"
     :top "-0.65em"
     :right (m/* vars/pad 0.70)
     :color "white"
     :font-size "1.5em"}]])

(defn mixins-button []
  [:&
   {:border-radius "0.25em"
    :border "1px solid #ccc"
    :text-decoration "none"
    :color "#aaa"
    :padding [[(em 0.1) (em 0.25)]]
    :white-space "nowrap"}
   [:&:hover
    {:color "#999"
     :border-color "#aaa"}]])

(defn mixins-header-text []
  {:text-transform "uppercase"
   :letter-spacing "0.1em"
   :font-weight "bold"
   :line-height header-height
   :-webkit-font-smoothing "antialiased"
   :padding [[0 (m/* vars/pad 0.75)]] })

(def quest-icon-size (rem 2))

(defn quests-header []
  [:&
   [:.quests-header
    {:position "relative"}

    [".bar:hover + .quests-menu"
     ".quests-menu:hover"
     {:display "inline-block"}]

    [:.bar
     (mixins-header-text)

     [:&:before
      (mixins/fontawesome \uf091)
      {:margin-right (em 0.5)}]]

    [:.quests-menu
     (mixins-context-menu)
     {:position "absolute"
      :top header-height
      :right 0
      :z-index 150
      :display "none"}

     [:.content

      [:.quest
       {:margin-bottom (em 2)
        :display "flex"
        :justify-content "space-between"}

       [:&:last-child
        {:margin-bottom 0}]

       [:&:before
        {:content "attr(data-icon)"
         :font-family "FontAwesome"
         :display "block"
         :font-size quest-icon-size
         :color "#666"
         :margin [[0 vars/pad 0 (m// vars/pad 2)]]
         :align-self "center"}]

       [:h1
        {:font-size (em 1)
         :margin 0}]

       [:p
        {:margin 0
         :width (em 18)}]

       [:.actions
        {:align-self "center"}
        [:a
         (mixins-button)
         {:margin-left (em 0.5)}]]]]]]])

(defn group-header []
  [:.group-header

   [:.bar
    {:display "inline-block"
     :vertical-align "top"}]

   [:.group-name
    :a
    {:color "white"
     :display "inline-block"
     :vertical-align "top"
     :height header-height
     :line-height header-height
     :-webkit-font-smoothing "antialiased"}]

   [:.group-name
    (mixins-header-text)
    {:min-width (em 5)}]

   [:a
    {:width header-height
     :text-align "center"
     :text-decoration "none"}

    [:&:hover
     :&.active
     {:background "rgba(0,0,0,0.25)"}]

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
      :height header-height
      :outline "none"}]

    [:.action
     [:&:after
      {:top 0
       :right (m/* vars/pad 0.75)
       :height header-height
       :line-height header-height
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
     {:height header-height
      :line-height header-height
      :margin-left (em 0.5)
      :font-size (em 1.5)}]

    [:&.error
     [:&:before
      (mixins/fontawesome\uf071)]]

    [:&.loading
     [:&:before
      (mixins/fontawesome \uf110)
      mixins/spin]]]])

(defn user-header []
  [:.user-header

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
      (mixins-header-text)
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

   [:.options
    (mixins-context-menu)
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
      :line-height "1.85em"}

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
      (mixins/fontawesome \uf013)]]]]
  )

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

     (group-header)
     [:.spacer
      {:flex-grow 2}]
     (quests-header)
     (user-header)]]])
