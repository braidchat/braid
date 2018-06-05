(ns braid.emoji.client.styles)

(def autocomplete
  [:.app>.main>.page>.threads>.thread>.card
   [:>.message.new
    [:>.autocomplete-wrapper
     [:>.autocomplete
      [:>.result
       [:>.emoji.match
        [:>img
         {:display "block"
          :width "2em"
          :height "2em"
          :float "left"
          :margin "0.25em 0.5em 0.25em 0"}]]]]]]])


