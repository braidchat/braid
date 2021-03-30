(ns braid.core.client.ui.styles.message
  (:require
   [braid.core.client.ui.styles.hljs :refer [hljs-styles]]
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :refer [avatar-size pad card-width]]
   [garden.arithmetic :as m]
   [garden.units :refer [rem px]]))

(def message-left-pad  (m/+ avatar-size (rem 0.5)))

(def message
  [:>.message
   {:position "relative"
    :padding [[(m// pad 2) pad]]}

   [:&.failed-to-send
    {:background-color "rgba(239, 72, 72, 0.53)"}]

   [:&.collapse
    {:margin-top (m/- pad)}

    [:>.avatar
     :>.info
     {:display "none"}]]

   [:&.unseen
    [:>.border
     (mixins/card-border "9px")]]

   [:>button.delete
    {:display "none"
     :position "absolute"
     :right pad
     :line-height "1.25em"
     :color "#ccc"
     :background "#fff"
     :width "1.25em"
     :height "1.25em"
     :border-radius "50%"
     :border "none"
     :padding 0
     :cursor "pointer"
     :margin-left "0.25rem"}
    (mixins/fontawesome nil)

    [:&:hover
     {:color "red"}]]

   [:&:hover
    [:>button.delete
     {:display "inline-block"}]]

   [:>.avatar
    [:>img
     {:cursor "pointer"
      :width avatar-size
      :height avatar-size
      :display "inline-block"
      :position "absolute"
      :border-radius "20%"}]]

   [:>.info
    {:height "1rem"
     :margin-left (m/+ avatar-size (rem 0.5))
     :overflow "hidden"
     :white-space "nowrap"}

    [:>.nickname
     {:display "inline"
      :font-weight "bold"
      :text-decoration "none"
      :color "#000"}]

    [:>.time
     {:display "inline"
      :margin-left "0.25rem"
      :color "#ccc"}]]

   [:>.content
    {:white-space "pre-wrap"
     :word-break "break-word"
     :display "inline-block"
     :padding-left message-left-pad
     :width "100%"
     :box-sizing "border-box"
     :line-height "1.25em"
     :margin-top (rem 0.20)}

    [:a.external
     mixins/pill-box
     {:background "#000000"
      :max-width "inherit !important"}

     [:&::before
      (mixins/fontawesome \uf0c1)
      {:display "inline"
       :margin-right "0.25em"
       :vertical-align "middle"
       :font-size "0.8em"}]]]

   [:pre
    {:background-color "black"
     :color "white"}

    hljs-styles

    [:&.inline
     {:padding "0.25em 0.5em"
      :display "inline-block"
      :text-overflow "ellipsis"
      :overflow-x "hidden"
      :max-width "100%"
      :vertical-align "middle"
      :margin 0}

     [:&:hover
      {:text-overflow "initial"
       :overflow-x "auto"}]]

    [:&.multiline
     {:display "block"
      :margin [[0 0 0 (m/* -1 (m/+ pad message-left-pad))]]
      :width card-width}

     [:>code
      {:overflow-x "hidden"
       :display "block"
       :box-sizing "border-box"
       :padding "1em"
       :width "100%"
       :text-overflow "ellipsis"}

      [:&:hover
       {:text-overflow "initial"
        :overflow-x "auto"}]]]]])
