(ns braid.core.client.bots.views.bots-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em]]))

(def bots-page
  [:>.page.bots
   [:>.title
    {:font-size "large"
     :text-transform "uppercase"}]
   [:>.content
    {:background-color "white"
     :color "black"}

    [:>.bots-list
     mixins/flex
     {:flex-direction "row"
      :flex-wrap "wrap"
      :align-content "space-between"
      :align-items "baseline"}

     [:>.bot
      {:margin (em 1)}
      [:img {:margin "0 auto"}]
      [:button
       (mixins/outline-button {:text-color "black"
                               :border-color "darkgray"
                               :hover-text-color "lightgray"})
       [:&.dangerous {:color "red"}]
       [:&.delete
        {:font-family "fontawesome"}]]

      [:>.avatar
       {:width (rem 4)
        :height (rem 4)
        :display "block"
        :border-radius (rem 1)
        :margin-bottom vars/pad}]]]

    [:>.add-bot
     {:max-width "50%"
      :margin "0 auto"}
     [:form
      mixins/flex
      {:flex-direction "column"
       :align-items "center"}
      [:.new-avatar>img
       {:max-width "150px"}]]]]])
