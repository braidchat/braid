(ns braid.client.gateway.forms.join-group.styles
  (:require
    [braid.client.gateway.styles-vars :as vars]
    [braid.client.ui.styles.fontawesome :as fontawesome]))

(defn join-group-styles []
  [:#app

   [:>.gateway

    [:>.join-group

     [:>form

      [:>fieldset

       [:>.group-info
        {:text-align "center"
         :margin-bottom "1em"}

        [:>img
         {:height "50px"
          :width "50px"
          :margin-bottom "0.5em"}]

        [:>h1
         {:font-weight "normal"
          :font-size "1em"
          :color vars/secondary-text-color
          :margin 0}

         [:>.name
          {:color vars/accent-color
           :font-weight "bold"}]]

        [:>p
         (vars/small-text-mixin)

         [:&.members-count
          [:&::before
           {:margin-right "0.5em"
            :-webkit-font-smoothing "antialiased"}
           (fontawesome/mixin :users)]]]]]]]]])
