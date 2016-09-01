(ns braid.client.group-admin.views.group-settings-page-styles
  (:require [garden.units :refer [rem em]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def group-settings-page
  [:.page.group-settings
   [:.setting
    [:&.avatar
     [:.upload
      [:.uploading-indicator
       {:display "inline-block"}]]]]])
