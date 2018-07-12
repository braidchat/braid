(ns braid.core.client.group-admin.views.group-settings-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def group-settings-page
  [:>.page.group-settings
   [:>.content
    mixins/flex
    {:flex-direction "column"
     :align-items "center"}
    [:>.setting
     {:background-color "white"
      :font-size (em 1.1)
      :width "50%"
      :margin (em 1)
      :padding (em 1)
      :border-radius (px 10)}
     [:button
      (mixins/outline-button {:text-color vars/darkgrey-text
                              :hover-text-color "lightgray"
                              :border-color "darkgray"
                              :hover-border-color "lightgray"})
      [:&:disabled
       {:background-color "darkgray"
        :color "gray"
        :border-color "darkgray"}]]
     [:&.avatar
      [:>.upload
       [:>.uploading-indicator
        {:display "inline-block"}]]]]]])
