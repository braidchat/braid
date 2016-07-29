(ns braid.client.ui.styles.thread
  (:require [braid.client.ui.styles.vars :as vars]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.mixins :as mixins]
            [garden.units :refer [px em rem]]))

(defn thread [pad]
  [:.thread
   mixins/flex
   {:margin-right pad

    :min-width vars/card-width
    :width vars/card-width
    :box-sizing "border-box"
    :outline "none"

    :flex-direction "column"
    :height "100%"
    :z-index 101}

   [:&.new
    {:z-index 99}]

   [:&:before ; switch to :after to align at top
    {:content "\"\""
     :flex-grow 1}]

   [:&.archived :&.limbo :&.private
    [:.head:before
     {:content "\"\""
      :display "block"
      :width "100%"
      :height (px 5)
      :position "absolute"
      :top 0
      :left 0
      :border-radius [[vars/border-radius
                       vars/border-radius 0 0]]}]

      [:&.archived
       [:.head:before
        {:background vars/archived-thread-accent-color}]]

      [:&.private
       [:.head:before
        {:background vars/private-thread-accent-color}]]

      [:&.limbo
       [:.head:before
        {:background vars/limbo-thread-accent-color}]]]

   [:&.focused
    [:.card
     {:box-shadow [[0 (px 10) (px 10) (px 10) "#ccc"]]}]]

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
      :max-height "85%"}]]

   [:&.private
    [:.notice
     {:background "#D2E7FF"
      :color vars/private-thread-accent-color}

     [:&:before
      (mixins/fontawesome \uf21b)]]]

   [:&.limbo
    [:.notice
     {:background "#ffe4e4"
      :color vars/limbo-thread-accent-color}

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

    [:.permalink
     [:button
      mixins/pill-button]]

    [:.controls
     {:position "absolute"
      :padding pad
      :top 0
      :right 0
      :z-index 10
      :color "#CCC"
      :text-align "right"}

     [:.control
      {:cursor "pointer"}

      [:&:hover
       {:color "#333"}]

      [:&.close
       [:&:after
        (mixins/fontawesome \uf00d)]]

      [:&.unread
       [:&:after
        (mixins/fontawesome \uf0e2)]]

      [:&.permalink
       [:&:after (mixins/fontawesome \uf0c1)]]
      [:&.mute
       [:&:after (mixins/fontawesome \uf1f6)]]

      [:&.hidden
       {:margin-top (m/* pad 0.5)
        :display "none"}

       [:&:after
        {:font-size "0.9em"
         :margin-right "-0.15em"}]]]]

    [".controls:hover > .hidden"
     {:display "block"}]]])

(defn messages [pad]
  [:.thread
   [:.messages
    {:position "relative"
     :overflow-y "scroll"
     :padding [[0 pad]]}]])

(defn new-message [pad]
  [:.message.new
   {:flex-shrink 0
    :padding pad
    :margin 0
    :position "relative"}

   [:.plus
    {:border-radius vars/border-radius
     :text-align "center"
     :line-height (em 2)
     :position "absolute"
     :top 0
     :bottom 0
     :left 0
     :width vars/avatar-size
     :margin pad
     :cursor "pointer"
     :color "#e6e6e6"
     :box-shadow "0 0 1px 1px #e6e6e6"}

    [:&:after
     {:position "absolute"
      :top "50%"
      :left 0
      :width "100%"
      :margin-top (em -1)}
      (mixins/fontawesome \uf067)]

    [:&:hover
     {:color "#ccc"
      :box-shadow "0 0 1px 1px #ccc"}]

    [:&:active
     {:color "#999"
      :box-shadow "0 0 1px 1px #999"}]

    [:&.uploading:after
     (mixins/fontawesome \uf110)
     mixins/spin]]

    [:textarea
     {:width "100%"
      :resize "none"
      :border "none"
      :box-sizing "border-box"
      :min-height (em 3.5)
      :padding-left (rem 2.5)}

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


   [:.uploading-indicator
    (mixins/fontawesome \uf110)
    mixins/spin
    {:font-size (em 1.5)
     :text-align "center"}]])
