(ns braid.client.ui.styles.page
  (:require [garden.units :refer [rem]]
            [braid.client.ui.styles.vars :as vars]))

(def page
  [:.page
   {:position "absolute"
    :left vars/sidebar-width
    :top vars/pad
    :bottom 0
    :right 0
    :margin-top vars/top-bar-height
    :overflow-x "scroll"}

   ["> .title"
    {:height vars/top-bar-height
     :line-height vars/top-bar-height
     :color vars/grey-text
     :margin [[vars/pad 0 0 vars/pad]]}]

   ["> .intro"
    {:color vars/grey-text
     :margin vars/pad
     ; make intro above threads, so you can click on clear inbox button
     :z-index 100
     :position "relative"}]

   ["> .content"
    {:padding vars/pad
     :color vars/grey-text}

    ["> .description"
     {:width vars/card-width
      ; we want description to appear above the new thread, but below other threads
      ; TODO: is there a way to make description clickable but not cover
      ; threads that are long enough?
      :z-index 100
      ; opacity & position are hacks to make z-index work
      :position "relative" ; z-index only applies if element has a position set
      :opacity 0.99 ; need to create a new rendering context b/c threads are absolute postioned
      }

     [:.avatar
      {:width (rem 4)
       :height (rem 4)
       :display "block"
       :border-radius (rem 1)
       :margin-bottom vars/pad}]]]])
