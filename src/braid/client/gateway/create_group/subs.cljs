(ns braid.client.gateway.create-group.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :gateway.action.create-group/sending?
  (fn [state _]
    (get-in state [:action :sending?])))
