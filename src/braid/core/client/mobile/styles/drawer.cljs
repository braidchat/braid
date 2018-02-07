(ns braid.core.client.mobile.styles.drawer
  (:require
    [braid.core.client.ui.styles.sidebar]
    [garden.arithmetic :as m]
    [garden.units :refer [rem em px ex]]))

(defn drawer [pad]
  [:>.drawer
   {:overflow-y "auto"}

   [:>.content
    {:width "100%"
     :padding pad
     :box-sizing "border-box"}

    [:>.sidebar

     [:>.groups

      [:>.group
       (braid.core.client.ui.styles.sidebar/sidebar-button "15vw")
       {:margin [[0 0 pad 0]]
        :color "#222"}]]]]])
