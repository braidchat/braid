(ns braid.client.mobile.styles.drawer
  (:require [braid.client.ui.styles.sidebar]
            [garden.units :refer [rem em px ex]]
            [garden.arithmetic :as m]))

(defn drawer [pad]
  [:.drawer

   [:.content
    {:width "100%"
     :padding pad
     :box-sizing "border-box"}

    (braid.client.ui.styles.sidebar/option "15vw")]])
