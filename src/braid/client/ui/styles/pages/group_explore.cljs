(ns braid.client.ui.styles.pages.group-explore
  (:require
    [garden.units :refer [rem em px]]
    [braid.client.ui.styles.mixins :as mixins]
    [braid.client.ui.styles.vars :as vars]))

(def group-explore-page
  [:.page.group-explore
   [:label
    {:display "block"}
    [:input
     {:display "block"}]]])
