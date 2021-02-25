(ns braid.core.client.gateway.subs
  (:require
   [re-frame.core :refer [reg-sub]]
   [braid.core.client.gateway.forms.user-auth.subs]))

(reg-sub
  ::action
  (fn [state _]
    (get-in state [:action])))

(reg-sub
  ::action-disabled?
  (fn [state _]
    (get-in state [:action-disabled?])))
