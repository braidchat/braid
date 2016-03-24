(ns braid.mobile.style
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-import]]
            [garden.arithmetic :as m]
            [garden.units :refer [vw vh]]))

(defn tee [x]
  (println x) x)

(def styles
  (css [:body]

       (let [w "100px"]
         [:.sidebar
          {:background "black"
           :position "absolute"
           :top 0
           :bottom 0
           :padding-left "100vw"
           :margin-left "-100vw"
           :left "-100px"
           :width w
           :z-index 100
           }
          [:&.open :&.closed
           {:transition "transform 0.25s ease-in-out"}]
          [:&.closed
           {:transform "translateX(0px)"}]
          [:&.open
           {:transform (str "translateX(" w ")")}]
          [:.content
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
              :color "white"}]]]])

       [:.page
        {:position "absolute"
         :top 0
         :left 0
         :right 0
         :bottom 0
         :background "red"
         :overflow-x "scroll"
         :overflow-y "hidden"
         :z-index 50
         }]

       (let [thread-margin 4 ;vw
             thread-peek 0 ;vw
             ]
         [:.threads
          {:height "100vh"
           :min-width "300vw"
           :padding-left (vw thread-margin)}
          [:.thread
           {:background "red"
            :width (vw (- 100 thread-margin thread-margin thread-peek))
            :height "100vh"
            :margin-right (vw thread-margin)
            :display "inline-block"
            :vertical-align "top"
            }]])))
