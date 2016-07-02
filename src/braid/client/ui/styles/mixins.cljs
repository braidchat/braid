(ns braid.client.ui.styles.mixins
  (:require [garden.units :refer [px em]]
            [braid.client.ui.styles.vars :as vars]))

(def flex
  {:display #{:flex :-webkit-flex}})

(def pill-box
  [:&
   {:font-size (em 0.75)
    :display "inline-block"
    :padding [[0 (em 0.5)]]
    :border-radius (em 0.5)
    :text-transform "uppercase"
    :letter-spacing (em 0.1)
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
    :text-align "center"}
  [:&.on
   {:color [["white" "!important"]]}]
  [:&.off
   {:background-color [["white" "!important"]]}]])

(def pill-button
  [:&
   pill-box
   [:&
    {:color vars/grey-text
     :border [[(px 1) "solid" vars/grey-text]]
     :background "none"}]

   [:&:hover
    {:color "#eee"
     :background vars/grey-text
     :cursor "pointer"}]

   [:&:active
    {:color "#eee"
     :background "#666"
     :border-color "#666"
     :cursor "pointer"}]])

(defn fontawesome [unicode]
  {:font-family "fontawesome"
   :content (str "\"" unicode "\"")})

(def spin
  {:animation [["anim-spin" "1s" "infinite" "steps(8)"]]
   :display "block"})

(defn box-shadow []
  {:box-shadow [[0 (px 1) (px 2) 0 "#ccc"]]})
