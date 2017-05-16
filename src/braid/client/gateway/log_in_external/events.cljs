(ns braid.client.gateway.log-in-external.events
  (:require
    [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :gateway.action.log-in-external/initialize
  (fn [_ _]
    {:dispatch-n [[:gateway.user-auth/initialize :log-in]]}))

(reg-event-fx
  :gateway.action.log-in-external/change-user-status
  (fn [{state :db} [_ logged-in?]]
    (when logged-in?
      (set! js/window.location "/"))
    {}))

