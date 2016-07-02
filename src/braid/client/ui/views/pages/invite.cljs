(ns braid.client.ui.views.pages.invite
  (:require [braid.client.routes :as routes]
            [braid.client.ui.views.group-invite :refer [group-invite-view]]))

(defn invite-page-view
  []
  [:div.page.me
   [:div.title "Invite"]
   [:div.content
    [group-invite-view]]])
