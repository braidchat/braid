(ns braid.mobile.style
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-import]]
            [garden.arithmetic :as m]
            [garden.units :refer [vw vh]]))

(defn tee [x]
  (println x) x)

(def styles
  (css [:body]

       [:.sidebar
        {:background "black"
         :position "absolute"
         :top 0
         :bottom 0
         :width "60%"
         :transition "left 0.25s ease-in-out"}
        [:&.closed
         {:left "-60%"}]
        [:&.open
         {:left 0}]
        [:.group
         {:display "block"}
         [:img
          {:width "2rem"
           :height "2rem"
           :background "white"
           :border-radius "10px"
          :vertical-align "middle"}]
         [:.name
          {:display "inline-block"
           :color "white"}]]]

       [:.page
        {:position "absolute"
         :top 0
         :left 0
         :right 0
         :bottom 0
         :background "grey"
         :overflow-x "scroll"
         :overflow-y "hidden"
         }]

       (let [thread-margin 4 ;vw
             thread-peek 0 ;vw
             ]
         [:.threads
          {:height "100vh"
           :min-width "300vw"
           :padding-left (vw thread-margin)}
          [:.thread
           {:background "white"
            :width (vw (- 100 thread-margin thread-margin thread-peek))
            :height "100vh"
            :margin-right (vw thread-margin)
            :display "inline-block"
            :vertical-align "top"
            }]])))
