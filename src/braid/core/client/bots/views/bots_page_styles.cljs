(ns braid.core.client.bots.views.bots-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em]]))

(def bots-page
  [:>.page.bots

   [:>.content

    [:>.bots-list
     mixins/flex
     {:flex-direction "row"
      :flex-wrap "wrap"
      :align-content "space-between"
      :align-items "baseline"}

     [:>.bot
      {:margin (em 1)}

      [:>.avatar
       {:width (rem 4)
        :height (rem 4)
        :display "block"
        :border-radius (rem 1)
        :margin-bottom vars/pad}]]]]])
