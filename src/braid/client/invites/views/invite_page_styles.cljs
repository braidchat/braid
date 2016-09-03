(ns braid.client.invites.views.invite-page-styles
  (:require [garden.units :refer [rem em px]]
            [braid.client.ui.styles.mixins :as mixins]
            [braid.client.ui.styles.vars :as vars]))

(def invite-page
  [:.page.invite
   [:.invite-form

    [:autocomplete
     {:background-color "#999"}

     [:.results
      {:list-style-type "none"
       :padding-left (px 10)}

      [:.result

       [:&:hover
        {:background "#eee"}]]

      [:.active
       {:font-weight "bold"}]]]] ])
