(ns braid.client.gateway.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [braid.client.gateway.forms.user-auth.subs]
    [braid.client.gateway.forms.create-group.subs]
    [braid.client.gateway.forms.join-group.subs]))

(reg-sub
  :gateway/action
  (fn [state _]
    (get-in state [:action])))

(reg-sub
  :gateway/action-disabled?
  (fn [state _]
    (get-in state [:action-disabled?])))
