(ns braid.mobile.style
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-import]]
            [garden.arithmetic :as m]
            [garden.units :refer [rem vw vh]]
            [braid.ui.styles.body]
            [braid.ui.styles.message]))

(defn tee [x]
  (println x) x)

(def pill-box
  [{:font-size "0.75em"
    :display "inline-block"
    :padding "0 0.5em"
    :border-radius "0.5em"
    :text-transform "uppercase"
    :letter-spacing "0.1em"
    :background-color "#222"
    :border "1px solid #222"
    :height "1.75em"
    :line-height "1.75em"
    :max-width "10em"
    :white-space "nowrap"
    :overflow "hidden"
    :color "white"
    :vertical-align "middle"
    :cursor "pointer"
    :text-decoration "none"
    :text-align "center"}
    [:&.on
     {:color "white !important"}]
    [:&.off
     {:background-color "white !important"}]])

(def styles
  (let [pad (vw 5)
        pads "5vw"]
    (css  [:body
           {:touch-action "none"}]

         [:.sidebar
          {:background "red"}
          [:.content
           {:width "100%"
            :padding pad
            :box-sizing "border-box"}
           (let [icon-w "15vw"]
             [:.group
              {:display "block"
               :width "100%"
               :margin-bottom "1rem"
               :position "relative"}
              [:&.active:before
               (let [w "2vw"]
                 {:content "\"\""
                  :display "block"
                  :width w
                  :background "#eee"
                  :height icon-w
                  :position "absolute"
                  :left (m/- pad)
                  :border-radius [[0 w w 0]]
                  })]
              [:img
               {:width icon-w
                :height icon-w
                :background "white"
                :border-radius "0.5rem"
                :vertical-align "middle"}]
              [:.badge
               {:font-size "0.8rem"
                :padding "0 0.5em"
                :border-radius "0.5em"
                :display "inline-block"
                :line-height "1.75em"
                :background "#B53737"
                :color "white"
                :border-color "#B53737"
                :position "absolute"
                :bottom "-0.5rem"
                :right "-0.5rem"}]])]]

         (vec (concat [:.tag] pill-box))

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
            :background "white" }
           [:.head
            {:min-height "3.5em"
             :position "relative"}
            [:.tags
             {; TODO use pad var
              :padding [[pad (vw 10) pad pad]]}
             (vec (concat [:.add] pill-box))
             [:.tag :.add
              {:margin-bottom "0.5em"
               :margin-right "0.5em"}]]
            [:.close
             {:position "absolute"
              :padding pad
              :top 0
              :right 0
              :z-index 10
              :cursor "pointer"}]]
           [:.messages
            {:position "relative"
             :overflow-y "scroll"
             :padding [[0 pad]]}]]]

         braid.ui.styles.body/body
         braid.ui.styles.message/message
         braid.ui.styles.message/new-message
         )))
