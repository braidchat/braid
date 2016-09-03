(ns braid.client.ui.styles.pages.me
  (:require [garden.units :refer [rem em px]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def me-page
  [:.page.me

   [:.nickname

    [:.error
     {:color "red"}]]

   [:.group

    [:.name
     {:color "#999"
      :margin-bottom (em 0.25)}]


    [:.new-tag.error
     {:border-color "red"}]]])
