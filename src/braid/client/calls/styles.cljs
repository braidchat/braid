(ns braid.client.calls.styles
  (:require [garden.units :refer [em px rem]]
            [garden.arithmetic :as m]
            [braid.client.ui.styles.vars :as vars]
            [braid.client.ui.styles.mixins :as mixins]))

(defn call-view []
  [:.call
   {:position "absolute"
    :top "20px"
    :left "20px"
    :width "200px"
    :height "200px"
    :z-index 1005
    :background "white"}])
