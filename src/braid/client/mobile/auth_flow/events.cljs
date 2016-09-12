(ns braid.client.mobile.auth-flow.events
  (:require [re-frame.core :refer [reg-event-db]]))

(reg-event-db
  :set-auth-flow
  (fn [state [_ method stage]]
    (-> state
        (assoc-in [:auth-flow :method] method)
        (assoc-in [:auth-flow :stage] stage))))
