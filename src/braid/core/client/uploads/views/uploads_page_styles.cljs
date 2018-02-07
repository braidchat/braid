(ns braid.core.client.uploads.views.uploads-page-styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em]]))

(def uploads-page
  [:>.page.uploads

   [:>.content

    [:>table.uploads
     {:width "100%"
      :flex-direction "row"
      :flex-wrap "wrap"
      :align-content "space-between"
      :align-item "bottom"}

     [:>tbody

      [:>tr.upload

       [:>td.uploaded-file

        [:>.embed
         {:width (rem 5)
          :margin 0}]

        [:>td.uploaded-thread
         {:height (rem 3)
          :margin (em 1)}

         [:>.tags
          {:display "inline"}]]]]]]]])
