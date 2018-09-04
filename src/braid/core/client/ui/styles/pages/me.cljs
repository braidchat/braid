(ns braid.core.client.ui.styles.pages.me
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def me-page
  [:>.page.me
   (mixins/settings-style)
   [:>.content
    [:>.setting
     [:form.password
      [:label
       {:display "block"}]]
     [:>.nickname
      [:input.error
       {:border-color "red"}]]]]])
