(ns braid.client.invites.views.invite-page
  (:require [braid.client.routes :as routes]
            [braid.client.invites.views.invite :refer [invite-view]]))

(defn invite-page-view
  []
  [:div.page.invite
   [:div.title "Invite"]
   [:div.content
    [invite-view]]])
