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
        {:background "red"}
        [:.content
         {:width "100%"
          :padding "1rem"
          :box-sizing "border-box"}
         [:.group
          {:display "block"
           :width "100%"}
          [:img
           {:width "100%"
            :padding-bottom "100%"
            :background "white"
            :border-radius "0.5rem"
            :vertical-align "middle"}]
          [:.name
           {:display "inline-block"
            :color "white"}]]]]

       [:.page
        {:position "absolute"
         :top 0
         :left 0
         :right 0
         :bottom 0
         :overflow "hidden"
         :z-index 50
         :background "#CCC"
         }]

       [:.threads
        [:.thread
         {:width "100vw"
          :height "100vh"
          :background "white" }]]))
