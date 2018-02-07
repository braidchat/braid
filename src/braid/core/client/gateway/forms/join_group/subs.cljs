(ns braid.core.client.gateway.forms.join-group.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [braid.core.client.gateway.helpers :as helpers]))

(reg-sub
  ::sending?
  (fn [state _]
    (get-in state [:join-group :sending?])))

(reg-sub
  ::error
  (fn [state _]
    (get-in state [:join-group :error])))

(reg-sub
  ::group
  (fn [state _]
    (get-in state [:join-group :group])))
