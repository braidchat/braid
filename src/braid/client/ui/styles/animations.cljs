(ns braid.client.ui.styles.animations
  (:require [garden.stylesheet :refer [at-keyframes]]))

(def anim-spin
  (at-keyframes :anim-spin
    ["0%" {:transform "rotate(0deg)"}]
    ["100%" {:transform "rotate(359deg)"}]))

