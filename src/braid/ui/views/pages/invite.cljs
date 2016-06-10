(ns braid.ui.views.pages.invite
  (:require [chat.client.routes :as routes]
            [braid.ui.views.group-invite :refer [group-invite-view]]))

(defn invite-page-view
  []
  [:div.page.me
   [:div.title "Invite"]
   [:div.content
    [group-invite-view]]])
