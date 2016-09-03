(ns braid.client.invites.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :invitations
  (fn [state _]
    (get-in state [:invitations])))
