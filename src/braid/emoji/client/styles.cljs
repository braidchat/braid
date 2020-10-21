(ns braid.emoji.client.styles)

(def autocomplete
  [:.app
   [:.message>.content
    [:.emoji.custom-emoji
     {:width "3ex"
      :height "3.1ex"
      :min-height "20px"
      :display "inline-block"
      :margin [["-0.2ex" "0.15em" "0.2ex"]]
      :line-height "normal"
      :vertical-align "middle"
      :min-width "20px"}]]
   [:>.main>.page>.threads>.thread>.card
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
           :margin "0.25em 0.5em 0.25em 0"}]]]]]]]])


