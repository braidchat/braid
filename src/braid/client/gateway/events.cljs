(ns braid.client.gateway.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.fx]
    [braid.client.gateway.user-auth.events]
    [braid.client.gateway.create-group.events]))

(reg-event-fx
  :gateway/initialize
  (fn [_ _]
    {:db {:action (keyword (aget js/window "gateway_action"))}
     :dispatch-n [[:gateway/initialize-action]]}))

(reg-event-fx
  :gateway/initialize-action
  (fn [{state :db} _]
    (case (state :action)
      :create-group
      {:dispatch-n [[:gateway.user-auth/initialize :register]
                    [:gateway.action.create-group/initialize]]}

      :log-in
      {:dispatch-n [[:gateway.user-auth/initialize :log-in]]})))
