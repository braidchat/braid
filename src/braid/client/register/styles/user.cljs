(ns braid.client.register.styles.user
  (:require
    [braid.client.register.styles.vars :as vars]
    [braid.client.ui.styles.fontawesome :as fontawesome]))

(defn user-styles []

  [:form

   [:.option

    [:.auth-providers
     {:display "inline"}

     [:a
      {:margin "0 0.4em"}

     [:&::before
      {:margin-right "0.25em"}]

     [:&.google
      [:&::before
       (fontawesome/mixin :google)]]

     [:&.github
      [:&::before
       (fontawesome/mixin :github)]]

     [:&.facebook
      [:&::before
       (fontawesome/mixin :facebook)]]]]

    [:&.password
     [:input
      {:width "15em"}]]

    [:&.email
     [:input
      {:width "15em"}]]]])

