(ns braid.core.client.ui.styles.pages.group-explore
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def group-explore-page
  [:.page.group-explore
   [:label
    {:display "block"}
    [:input
     {:display "block"}]]])
