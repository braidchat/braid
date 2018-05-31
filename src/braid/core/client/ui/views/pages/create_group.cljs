(ns braid.core.client.ui.views.pages.create-group
  (:require
   [braid.core.client.gateway.forms.create-group.views :refer [create-group-view]]
   [braid.core.client.gateway.forms.user-auth.views :refer [user-auth-view]]
   [braid.core.client.routes :as routes]
   [re-frame.core :refer [subscribe]]))

(defn create-group-page-view
  []
  [:div.page.create-group
   [:div.title "Create New Group"]
   [:div.content
    [:a {:href (routes/other-path {:page-id "group-explore"})}
     "Explore Groups"]
    [:div.gateway
     (when-not @(subscribe [:user-id])
       [user-auth-view])
     [create-group-view]]]])
