(ns braid.core.client.gateway.forms.log-in.events
  (:require
   [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  ::initialize
  (fn [_ _]
    {:dispatch-n [[:braid.core.client.gateway.forms.user-auth.events/initialize :log-in]]}))

(reg-event-fx
  ::change-user-status
  (fn [{state :db} [_ logged-in?]]
    (if logged-in?
      {:dispatch [:start-socket]}
      {})))
