(ns braid.uploads-page.views.uploads-page-styles
  (:require
   [garden.units :refer [rem em]]
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]))

(defn >uploads-page []
  [:>.page.uploads

   [:>.content

    [:button
     (mixins/settings-button)]

    [:>table.uploads
     {:width "100%"
      :flex-direction "row"
      :flex-wrap "wrap"
      :align-content "space-between"
      :align-item "bottom"}

     [:>tbody

      [:>tr.upload

       ["&:nth-child(odd)"
        {:background-color "#f9f9f9"}]

       ["&:nth-child(even)"
        {:background-color "white"}]

       [:>td.delete

        [:>button
         {:font-family "fontawesome"
          :color "red"}]]

       [:>td.uploaded-file

        [:img :video
         {:width (rem 5)
          :margin 0}]

        [:>td.uploaded-thread
         {:height (rem 3)
          :margin (em 1)}

         [:>.tags
          {:display "inline"}]]]]]]]])
