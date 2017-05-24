(ns braid.client.gateway.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.fx]
    [braid.client.gateway.forms.user-auth.events]
    [braid.client.gateway.forms.create-group.events]
    [braid.client.gateway.forms.join-group.events]
    [braid.client.gateway.forms.log-in.events]))

(reg-event-fx
  :gateway/initialize
  (fn [{state :db} [_ action]]
    {:db (-> state
             (assoc :action action)
             (assoc :action-disabled? true))
     :dispatch [:gateway/handle-action]}))

(reg-event-fx
  :gateway/handle-action
  (fn [{state :db} _ ]
    (case (state :action)
      :create-group
      {:dispatch [:gateway.action.create-group/initialize]}

      :log-in
      {:dispatch [:gateway.action.log-in/initialize]}

      :request-password-reset
      {:dispatch [:gateway.user-auth/initialize :request-password-reset]}

      :join-group
      {:dispatch [:gateway.forms.join-group/initialize]})))

(reg-event-fx
  :gateway/change-user-status
  (fn [{state :db} [_ logged-in?]]
    (merge
      {:db (assoc state :action-disabled? (not logged-in?))}
      (case (state :action)
        :log-in
        {:dispatch [:gateway.action.log-in/change-user-status logged-in?]}
        {}))))

