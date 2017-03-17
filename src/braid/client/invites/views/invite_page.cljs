(ns braid.client.invites.views.invite-page
  (:require
    [braid.client.invites.views.invite :refer [invite-view]]
    [braid.client.routes :as routes]))

(defn invite-page-view
  []
  [:div.page.invite
   [:div.title "Invite"]
   [:div.content
    [invite-view]]])
