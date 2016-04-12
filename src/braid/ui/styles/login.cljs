(ns braid.ui.styles.login
  (:require [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]
            [braid.ui.styles.mixins :as mixins]
            [braid.ui.styles.vars :as vars]))
(def login
  [:.login
   mixins/floating-box

   [:input
    {:width "100%"
     :margin [[0 0 (em 1)]]
     :padding (em 0.25)
     :box-sizing "border-box"}]])
