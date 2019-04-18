(ns braid.bots.client.views.bots-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def bot-notice
  [:>.thread>.card>.messages>.message>.info>.bot-notice
   {:background-color "#c0afc0"
    :border-radius (px 5)
    :padding (rem 0.25)
    :font-weight "bold"
    :color "#413f42"}])

(def bots-page
  [:.app>.main>.page.bots
   [:>.title
    {:font-size "large"}]
   [:>.content
    (mixins/settings-container-style)

    [:>.bots
     (mixins/settings-item-style)
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
        (mixins/outline-button {:text-color vars/grey-text
                                :border-color "darkgray"
                                :hover-border-color "lightgray"
                                :hover-text-color "lightgray"})
        [:&.dangerous {:color "red"}]
        [:&.delete
         {:font-family "fontawesome"}]]

       [:>.avatar
        {:width (rem 4)
         :height (rem 4)
         :display "block"
         :border-radius (rem 1)
         :margin-bottom vars/pad}]]]]

    [:>.add-bot
     (mixins/settings-item-style)
     {:max-width "50%"
      :margin "0 auto"}
     [:form
      mixins/flex
      {:flex-direction "column"}
      [:.new-avatar>img
       {:max-width "150px"}]]]]])
