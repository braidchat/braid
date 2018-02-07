(ns braid.core.client.invites.views.invite-page
  (:require
   [braid.core.client.invites.views.invite :refer [invite-view]]
   [braid.core.client.routes :as routes]))

(defn invite-page-view
  []
  [:div.page.invite
   [:div.title "Invite"]
   [:div.content
    [invite-view]]])
