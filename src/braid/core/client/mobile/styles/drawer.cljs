(ns braid.core.client.mobile.styles.drawer
  (:require
    [braid.sidebar.styles]
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
       (braid.sidebar.styles/sidebar-button "15vw")
       {:margin [[0 0 pad 0]]
        :color "#222"}]]]]])
