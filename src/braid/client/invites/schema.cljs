(ns braid.client.invites.schema
  (:require [braid.common.schema :as app-schema]))

(def init-state
  {:invitations []})

(def InvitesAppState
  {:invitations [app-schema/Invitation]})
