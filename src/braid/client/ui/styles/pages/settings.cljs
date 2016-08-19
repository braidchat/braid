(ns braid.client.ui.styles.pages.settings
  (:require [garden.units :refer [rem em]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def settings-page
  [:.page.settings
   [:.setting
    [:&.avatar
     [:.upload
      [:.uploading-indicator
       {:display "inline-block"}]]]]])
