(ns braid.client.ui.styles.pages.channels
  (:require [garden.units :refer [rem em]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def channels-page
  [:.page.channels

   [:.tags
    {:margin-top (em 1)
     :color vars/grey-text}

    [:.tag-info
     {:margin-bottom (em 1)}

     ["> .button"
      mixins/pill-button
      {:margin-left (em 1)}]]]

    [:.count
     {:margin-right (em 0.5)}
      [:&:after
       {:margin-left (em 0.25)}]

      [:&.threads-count
       [:&:after
        (mixins/fontawesome \uf181)]]

      [:&.subscribers-count
       [:&:after
        (mixins/fontawesome \uf0c0)]]]])
