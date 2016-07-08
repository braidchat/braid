(ns braid.client.ui.views.pages.me
  (:require [braid.client.routes :as routes]))

(defn me-page-view
  []
  [:div.page.me
   [:div.title "Me!"]
   [:div.content
    [:p "Placeholder page for group-related profile settings"]

    [:p
     [:a {:href (routes/other-path {:page-id "global-settings"})}
      "Go to Global Settings"]]]])
