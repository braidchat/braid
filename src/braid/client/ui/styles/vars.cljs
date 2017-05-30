(ns braid.client.ui.styles.vars
  (:require
    [garden.arithmetic :as m]
    [garden.units :refer [rem px]]))

(def avatar-size (rem 2))

(def pad (rem 1))

(def card-width (rem 18))

(def top-bar-height (rem 2))

(def sidebar-width
  (m/+ top-bar-height (m/* pad 2)))

(def grey-text "#888")

(def border-radius (px 3))

(def private-thread-accent-color "#5f7997")

(def limbo-thread-accent-color "#CA1414")

(def archived-thread-accent-color "#AAAAAA")

(def page-background-color "#f3f3f3")
(def primary-text-color    "#444444")
