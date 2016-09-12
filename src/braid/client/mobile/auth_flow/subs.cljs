(ns braid.client.mobile.auth-flow.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :auth-flow-method
  (fn [state _]
    (get-in state [:auth-flow :method])))

(reg-sub
  :auth-flow-stage
  (fn [state _]
    (get-in state [:auth-flow :stage])))

