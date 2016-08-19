(ns braid.client.ui.styles.pills
  (:require [braid.client.ui.styles.mixins :as mixins]))

(def tag
  [:.tag
   mixins/pill-box])

(def user
  [:.user
   mixins/pill-box
   [:&.admin:before
    (mixins/fontawesome \uf0e3)]])
