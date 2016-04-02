(ns braid.ui.styles.message
  (:require [braid.ui.styles.vars :refer [avatar-size pad card-width]]
            [garden.arithmetic :as m]
            [garden.units :refer [rem]]))

(def message
  [:.message
   {:position "relative"
    :margin [[0 0 pad]]}

   [:&.collapse
    {:margin-top (m/- pad)}
     [:.avatar :.info
      {:display "none"}]]

    [:&.seen
     {:transition [["opacity" "0.5s" "linear"]]
      :opacity 0.6}]

    [:&.unseen {}]

   ["> .avatar img"
    {:cursor "pointer"
     :width avatar-size
     :height avatar-size
     :display "inline-block"
     :position "absolute"
     :border-radius "20%"}]

   [:.info
    {:height "1rem"
     :margin-left (m/+ avatar-size (rem 0.5))
     :overflow "hidden"
     :white-space "nowrap"}
     [:.nickname
      {:display "inline"
       :font-weight "bold"
       :text-decoration "none"
       :color "#000"}]
     [:.time
      {:display "inline"
       :margin-left "0.25rem"
       :color "#ccc"}]]

   [:.content
    {:white-space "pre-wrap"
     :word-break "break-word"
     :display "inline-block"
     :padding-left (m/+ avatar-size (rem 0.5))
     :width "100%"
     :box-sizing "border-box"
     :line-height "1.25em"}

     [:.embedded-image
      {:max-width "100px"
       :max-height "100px"
       :transition "max-width 0.5s, max-height 0.5s"}
       [:&:hover
        {:max-width "3000px"
         :max-height "3000px"}]]]

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

