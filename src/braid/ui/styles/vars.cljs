(ns braid.ui.styles.vars
  (:require [garden.units :refer [rem px]]
            [garden.arithmetic :as m]))

(def avatar-size (rem 2))

(def pad (rem 1))

(def card-width (rem 17))

(def top-bar-height (rem 2))

(def sidebar-width
  (m/+ top-bar-height (m/* pad 2)))

(def grey-text "#888")

(def dark-bg-color "#3A4767")

(def border-radius (px 3))
