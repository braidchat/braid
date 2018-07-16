(ns braid.core.client.ui.styles.mixins
  (:require
   [braid.core.client.ui.styles.vars :as vars]
   [garden.arithmetic :as m]
   [garden.units :refer [px em rem]]))

(def flex
  {:display #{:flex :-webkit-flex}})

(def standard-font
  {:font-family "\"Open Sans\", Helvetica, Arial, sans-serif"
   :font-size "12px"})

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
    :box-sizing "content-box"
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

(defn fontawesome [unicode]
  {:font-family "fontawesome"
   :content (str "\"" unicode "\"")})

(defn outline-button
  [{:keys [text-color border-color
           hover-text-color hover-border-color
           icon]}]
  [:&
   {:display "inline-block"
    :background "none"
    :border-radius "0.25em"
    :border [["1px" "solid" border-color]]
    :text-decoration "none"
    :color text-color
    :padding [[0 (em 0.25)]]
    :box-sizing "content-box"
    :line-height "1.5em"
    :height "1.5em"
    :white-space "nowrap"
    :cursor "pointer"
    :text-align "center"}

   [:&:hover
    {:color hover-text-color
     :border-color hover-border-color}]
   (when icon
     [:&:after
      (fontawesome icon)
      {:margin-left "0.25em"}])])

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
    {:overflow-x "auto"
     :height "100%"
     :box-sizing "border-box"
     :padding [[(m/* vars/pad 0.75)]]}]

   ; triangle
   [:&::before
    (fontawesome \uf0d8)
    {:position "absolute"
     :top "-0.65em"
     :right (m/* vars/pad 0.70)
     :color "white"
     :font-size "1.5em"}]])

(defn card-border [width]
  {:position "absolute"
   ; background overriden inline
   :background "#000"
   :top 0
   :left 0
   :bottom 0
   :width width})

(defn settings-button []
  [:&
   (outline-button {:text-color vars/darkgrey-text
                    :hover-text-color "lightgray"
                    :border-color "darkgray"
                    :hover-border-color "lightgray"})
   [:&:disabled
    {:background-color "darkgray"
     :color "gray"
     :border-color "darkgray"}]])

(defn settings-item-style
  []
  [:&
   {:background-color "white"
    :font-size (rem 0.9)
    :width "50%"
    :margin (rem 1)
    :padding (rem 1)
    :border-radius (px 10)}
   [:>h2
    {:margin 0}]
   [:button (settings-button)]
   [:&.avatar
    [:>.upload
     [:>.uploading-indicator
      {:display "inline-block"}]]]])

(defn settings-container-style
  []
  [:&
   flex
   {:flex-direction "column"
    :align-items "center"}])

(defn settings-style []
  [:&
   [:>.content
    (settings-container-style)
    [:>.setting
     (settings-item-style)]]])
