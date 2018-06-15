(ns braid.core.client.mobile.auth-flow.events
  (:require
   [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :set-auth-flow
  (fn [{db :db} [_ method stage]]
    {:db (-> db
             (assoc-in [:auth-flow :method] method)
             (assoc-in [:auth-flow :stage] stage))}))
