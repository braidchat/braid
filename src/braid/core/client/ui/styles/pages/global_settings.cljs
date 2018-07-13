(ns braid.core.client.ui.styles.pages.global-settings
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [garden.units :refer [rem px]]))

(def global-settings-page
  [:>.page.global-settings
   [:>.title {:font-size "large"}]
   (mixins/settings-style)])
