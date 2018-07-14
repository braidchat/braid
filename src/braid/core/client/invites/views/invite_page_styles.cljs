(ns braid.core.client.invites.views.invite-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def invite-page
  [:>.page.invite
   [:>.content
    mixins/flex
    {:flex-direction "column"
     :align-items "center"}
    [:>.invite
     {:background-color "white"
      :border-radius (px 10)
      :padding (rem 1)
      :font-size (rem 0.9)
      :width "50%"}
     [:button (mixins/settings-button)]
     [:.invite-link
      [:>input
       {:width "100%"}]]]]])
