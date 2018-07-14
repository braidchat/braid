(ns braid.core.client.ui.styles.pages.tags
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [px rem em]]))

(def tags-page
  [:>.page.tags
   [:>.title
    {:font-size "large"}]
   [:>.content
    (mixins/settings-container-style)
    [:>.new-tag
     (mixins/settings-item-style)
     [:input.error
      {:color "red"}]]
    [:>.tag-list
     (mixins/settings-item-style)
     [:>.tags
      {:margin-top (em 1)
       :color vars/grey-text}

      [:>.tag-info
       {:margin-bottom (em 1)}

       [:>.button
        mixins/pill-button
        {:margin-left (em 1)}]

       [:>.count
        {:margin-right (em 0.5)}

        [:&::after
         {:margin-left (em 0.25)}]

        [:&.threads-count
         [:&::after
          (mixins/fontawesome \uf181)]]

        [:&.subscribers-count
         [:&::after
          (mixins/fontawesome \uf0c0)]]]]]]]])
