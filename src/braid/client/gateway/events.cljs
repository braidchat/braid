(ns braid.client.gateway.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.fx]
    [braid.client.gateway.forms.user-auth.events]
    [braid.client.gateway.forms.create-group.events]
    [braid.client.gateway.forms.join-group.events]
    [braid.client.gateway.forms.log-in.events]))

(reg-event-fx
  ::initialize
  (fn [{state :db} [_ action]]
    {:db (-> state
             (assoc :action action)
             (assoc :action-disabled? true))
     :dispatch [::handle-action]}))

(reg-event-fx
  ::handle-action
  (fn [{state :db} _ ]
    (case (state :action)
      :create-group
      {:dispatch [:braid.client.gateway.forms.create-group.events/initialize]}

      :log-in
      {:dispatch [:braid.client.gateway.forms.log-in.events/initialize]}

      :request-password-reset
      {:dispatch [:braid.client.gateway.forms.user-auth.events/initialize :request-password-reset]}

      :join-group
      {:dispatch [:braid.client.gateway.forms.join-group.events/initialize]})))

(reg-event-fx
  ::change-user-status
  (fn [{state :db} [_ logged-in?]]
    (merge
      {:db (assoc state :action-disabled? (not logged-in?))}
      (case (state :action)
        :log-in
        {:dispatch [:braid.client.gateway.forms.log-in.events/change-user-status logged-in?]}
        {}))))

