(ns braid.client.gateway.log-in.events
  (:require
    [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :gateway.action.log-in/initialize
  (fn [_ _]
    {:dispatch-n [[:gateway.user-auth/initialize :log-in]]}))

(reg-event-fx
  :gateway.action.log-in/change-user-status
  (fn [{state :db} [_ logged-in?]]
    (if logged-in?
      {:dispatch [:start-socket]}
      {})))

