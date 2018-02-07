(ns braid.core.client.invites.schema
  (:require
   [braid.core.common.schema :as app-schema]))

(def init-state
  {:invitations []})

(def InvitesAppState
  {:invitations [app-schema/Invitation]})
