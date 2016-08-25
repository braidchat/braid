(ns braid.client.ui.styles.message
  (:require [braid.client.ui.styles.vars :refer [avatar-size pad card-width]]
            [braid.client.ui.styles.mixins :as mixins]
            [garden.arithmetic :as m]
            [garden.units :refer [rem px]]))

(def message
  [:.message
   {:position "relative"
    :margin [[0 0 pad]]}

   [:&.failed-to-send
    {:background-color "rgba(239, 72, 72, 0.53)"}]

   [:&.collapse
    {:margin-top (m/- pad)}
     [:.avatar :.info
      {:display "none"}]]

    [:&.seen
     {:transition [["opacity" "0.5s" "linear"]]}
     ; cannot apply opacity to .message or .content or .dummy
     ; b/c it creates a new stacking context
     ; causing hover-cards to display under subsequent messages
     ["> .info"
      "> .avatar img"
      "> .embed"
      "> .content > img"
      "> .content > .dummy > .user > .pill"
      "> .content > .dummy > .tag > .pill"
      "> .content > .dummy > .external"
      {:opacity 0.6}]
     ["> .content"
      {:color "rgba(0,0,0,0.6)"}]]

    [:&.unseen {}]

   ["> .avatar img"
    {:cursor "pointer"
     :width avatar-size
     :height avatar-size
     :display "inline-block"
     :position "absolute"
     :border-radius "20%"}]

   ["> .info"
    {:height "1rem"
     :margin-left (m/+ avatar-size (rem 0.5))
     :overflow "hidden"
     :white-space "nowrap"}

    [:.bot-notice
     {:background-color "#c0afc0"
      :border-radius (px 5)
      :padding (rem 0.25)
      :font-weight "bold"
      :color "#413f42"}]

    [:.nickname
     {:display "inline"
      :font-weight "bold"
      :text-decoration "none"
      :color "#000"}]

    [:.time
     {:display "inline"
      :margin-left "0.25rem"
      :color "#ccc"}]]

   ["> .content"
    {:white-space "pre-wrap"
     :word-break "break-word"
     :display "inline-block"
     :padding-left (m/+ avatar-size (rem 0.5))
     :width "100%"
     :box-sizing "border-box"
     :line-height "1.25em"
     :margin-top (rem 0.20)}

    [:a.external
     mixins/pill-box
     {:background "#000000"
      :max-width "inherit !important"}

     [:&:before
      (mixins/fontawesome \uf0c1)
      {:display "inline"
       :margin-right "0.25em"
       :vertical-align "middle"
       :font-size "0.8em"}]]]

    [:.prettyprint
     {:background-color "#e2f5ff"
      :color "black"}
      [:.opn :.clo
       {:color "#e28964"}]
      [:.kwd
       {:color "#785c28"}]
      [:.str
       {:color "green"}]
      [:.typ
       {:color "rgb(65, 125, 255)"}]]

    [:.multiline-code
     {:display "block"
      :text-overflow "ellipsis"
      :white-space "pre"
      :word-break "normal"
      :width "100%"
      :overflow-x "hidden"}
      [:&:hover
       {:text-overflow "initial"
        :overflow-x "scroll"}]]])
