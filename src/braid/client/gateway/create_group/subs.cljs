(ns braid.client.gateway.create-group.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :gateway.action.create-group/sending?
  (fn [state _]
    (get-in state [:action :sending?])))

(reg-sub
  :gateway.action.create-group/error
  (fn [state _]
    (get-in state [:action :error])))
