(ns braid.group-explore.styles
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.units :refer [rem em px]]))

(def >group-explore-page
  [:>.page.group-explore

   [:>.content
    (mixins/settings-container-style)
    [:>.invitations
     (mixins/settings-item-style)]
    [:>.public-groups
     (mixins/settings-item-style)

     [:>.active
      :>.stale
      {:display "flex"
       :flex-wrap "wrap"}

      [:>a.group
       {:color "white"
        :padding "0.5em"
        :margin "0.2em"
        :border-radius (px 5)
        :text-decoration "none"}
       [:&:hover
        {:color "#ccc"
         :opacity 0.8}]

       [:>.name
        {:font-size "large"}]

       [:>.info
        {:font-size "small"}]]]

     [:>h3

      [:>button.toggle-stale
       mixins/standard-font
       {:background "transparent"
        :background-color "darkgray"
        :cursor "pointer"
        :border "none"
        :color "white"
        :margin-left "1rem"}]]]]])
