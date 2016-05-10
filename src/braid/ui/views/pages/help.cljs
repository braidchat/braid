(ns braid.ui.views.pages.help)

(defn help-page-view
  []
  [:div.page.help
   [:div.title "Help"]

   [:div.content
    [:div.description
     [:p "One day, a help page will be here."]
     [:p "If something is broken or confusing, please let us know by "
      [:a {:href "https://github.com/braidchat/meta/issues" :target "_blank" :rel "noopener noreferrer"}
       "Opening an issue"]]]]])
