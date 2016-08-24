(ns braid.client.ui.styles.mixins
  (:require [garden.units :refer [px em]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.vars :as vars]))

(def flex
  {:display #{:flex :-webkit-flex}})

(defn mini-text []
  {:font-size (em 0.75)
   :text-transform "uppercase"
    :letter-spacing (em 0.1)})

(def pill-box
  [:&
   {:display "inline-block"
    :padding [[0 (em 0.5)]]
    :border-radius (em 0.5)
    :background-color "#222"
    :border [[(px 1) "solid" "#222"]]
    :height (em 1.75)
    :line-height (em 1.75)
    :max-width (em 10)
    :white-space "nowrap"
    :overflow "hidden"
    :color "white"
    :vertical-align "middle"
    :cursor "pointer"
    :text-decoration "none"
    :text-align "center"
    :outline "none"}
   (mini-text)
  [:&.on
   {:color [["white" "!important"]]}]
  [:&.off
   {:background-color [["white" "!important"]]}]])

(def pill-button
  [:&
   pill-box
   [:&
    {:color "#888"
     :border [[(px 1) "solid" "#BBB"]]
     :background "none"}]

   [:&:hover
    {:color "#EEE"
     :background "#888"
     :border-color "#888"
     :cursor "pointer"}]

   [:&:active
    {:color "#EEE"
     :background "#666"
     :border-color "#666"
     :cursor "pointer"}]])

(defn outline-button []
  [:&
   {:display "inline-block"
    :border-radius "0.25em"
    :border "1px solid #ccc"
    :text-decoration "none"
    :color "#aaa"
    :padding [[(em 0.1) (em 0.25)]]
    :white-space "nowrap"
    :cursor "pointer"
    :text-align "center"}
   [:&:hover
    {:color "#999"
     :border-color "#aaa"}]])

(defn fontawesome [unicode]
  {:font-family "fontawesome"
   :content (str "\"" unicode "\"")})

(def spin
  {:animation [["anim-spin" "1s" "infinite" "steps(8)"]]
   :display "block"})

(defn box-shadow []
  {:box-shadow [[0 (px 1) (px 2) 0 "#ccc"]]})

(defn context-menu []
  [:&
   {:background "white"
    :border-radius vars/border-radius}
   (box-shadow)

   [:.content
    {:overflow-x "scroll"
     :height "100%"
     :box-sizing "border-box"
     :padding [[(m/* vars/pad 0.75)]]}]

   ; triangle
   [:&:before
    (fontawesome \uf0d8)
    {:position "absolute"
     :top "-0.65em"
     :right (m/* vars/pad 0.70)
     :color "white"
     :font-size "1.5em"}]])

