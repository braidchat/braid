(ns braid.embeds.styles
  (:require
   [garden.arithmetic :as m]
   [braid.core.client.ui.styles.vars :as vars]))

(def embed
  [:.embed
   {:margin [[0 (m/* -1 vars/pad)]]
    :padding [[vars/pad 0 0]]
    :overflow "hidden"
    :cursor "pointer"}])
