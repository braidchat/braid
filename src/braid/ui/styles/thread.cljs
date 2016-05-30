(ns braid.ui.styles.thread
  (:require [braid.ui.styles.vars :as vars]
            [garden.arithmetic :as m]
            [braid.ui.styles.mixins :as mixins]
            [garden.units :refer [px em rem]]))

(defn thread [pad]
  [:.thread
   mixins/flex
   {:margin-right pad

    :min-width vars/card-width
    :width vars/card-width
    :box-sizing "border-box"

    :flex-direction "column"
    :height "100%"
    :z-index 101}

   [:&.new
    {:z-index 99}]
   [:&:before ; switch to :after to align at top
    {:content "\"\""
     :flex-grow 1}]

   [:.card
    mixins/flex
    {:flex-direction "column"
     :box-shadow [[0 (px 1) (px 2) 0 "#ccc"]]
     :transition [["box-shadow" "0.2s"]]
     :max-height "100%"
     :background "white"
     :border-radius [[vars/border-radius
                      vars/border-radius 0 0]]}]])

(defn notice [pad]
  [:.thread

   [:.notice
    {:box-shadow [[0 (px 1) (px 2) 0 "#ccc"]]
     :padding pad
     :margin-bottom pad}

    [:&:before
     {:float "left"
      :font-size vars/avatar-size
      :margin-right (rem 0.5)
      :content "\"\""}]]

   [:&.private :&.limbo
    [:.card
     {; needs to be a better way
      ; which is based on the height of the notice
      :max-height "85%"}]

    [:.head:before
     {:content "\"\""
      :display "block"
      :width "100%"
      :height (px 5)
      :position "absolute"
      :top 0
      :left 0}]]

   [:&.private
    [:.head:before
     {:background "#5f7997"}]

    [:.notice
     {:background "#D2E7FF"
      :color "#5f7997"}

     [:&:before
      (mixins/fontawesome \uf21b)]]]

   [:&.limbo
    [:.head:before
     {:background "#CA1414"}]

    [:.notice
     {:background "#ffe4e4"
      :color "#CA1414"}

     [:&:before
      (mixins/fontawesome \uf071)]]]])

(defn head [pad]
  [:.thread
   [:.head
    {:min-height "3.5em"
     :position "relative"
     :width "100%"
     :flex-shrink 0
     :padding [[pad (m/* 2 pad) pad pad]]
     :box-sizing "border-box"}

    [:.tags

     [:.add
      mixins/pill-box]

     [:.user :.tag :.add
      {:margin-bottom (em 0.5)
       :margin-right (em 0.5)} ] ]

    [:.close
     {:position "absolute"
      :padding pad
      :top 0
      :right 0
      :z-index 10
      :cursor "pointer"}]]])

(defn messages [pad]
  [:.thread
   [:.messages
    {:position "relative"
     :overflow-y "scroll"
     :padding [[0 pad]]}]])

(defn new-message [pad]
  [:.message.new
   {:flex-shrink 0
    :paddgin-bottom pad
    :padding-left (m/+ pad (rem 2))
    :padding-top pad
    :padding-right pad
    :margin-bottom pad}

    [:textarea
     {:width "100%"
      :resize "none"
      :border "none"
      :box-sizing "border-box"
      :min-height (em 3.5)
      :box-shadow "0 0 1px 1px #ccc"
      :border-radius vars/border-radius}

      [:&:focus
       {:outline "none"}]]

    [:.autocomplete
     {:z-index 1000
      :box-shadow [[0 (px 1) (px 4) 0 "#ccc"]]
      :background "white"
      :max-height (em 20)
      :overflow "scroll"
      :width vars/card-width
      ; will be an issue when text area expands:
      :position "absolute"
      :bottom (m/* pad 3)}

      [:.result
       {:padding "0.25em 0.5em"
        :clear "both"}
       [:.emojione :.avatar :.color-block
        {:display "block"
         :width "2em"
         :height "2em"
         :float "left"
         :margin "0.25em 0.5em 0.25em 0"}]

       [:.color-block
        {:width "1em"}]

       [:.name
        {:height "1em"
         :white-space "nowrap"}]

       [:.extra
        {:color "#ccc"
         :overflow-y "hidden"
         :max-height "2.5em"}]

       [:&:hover
        {:background "#eee"}]

       [:&.highlight
        [:.name
         {:font-weight "bold"}]]]]])

(defn drag-and-drop [pad]
  [:.thread :.setting.avatar

   [:&.dragging
    {:background-color "gray"
     :border [[(px 5) "dashed" "black"]]}]

   [:&.focused
    [:.card
     {:box-shadow [[0 (px 10) (px 10) (px 10) "#ccc"]]}]]

   [:.uploading-indicator
    (mixins/fontawesome \uf110)
    mixins/spin
    {:font-size (em 1.5)
     :text-align "center"}]])