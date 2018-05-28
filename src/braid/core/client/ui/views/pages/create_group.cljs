(ns braid.core.client.ui.views.pages.create-group
  (:require
   [braid.core.client.gateway.forms.create-group.views :refer [create-group-view]]
   [braid.core.client.routes :as routes]))

(defn create-group-page-view
  []
  [:div.page.create-group
   [:div.title "Create New Group"]
   [:div.content
    [:a {:href (routes/other-path {:page-id "group-explore"})}
     "Explore Groups"]
    [:div.gateway
     [create-group-view]]]])
