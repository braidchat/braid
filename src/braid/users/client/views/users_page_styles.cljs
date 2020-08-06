(ns braid.users.client.views.users-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def users-page
  [:.app>.main>.page.users
   [:>.title
    {:font-size "large"}]

   [:>.content
    (mixins/settings-container-style)

    [:>.users
     (mixins/settings-item-style)]]])
