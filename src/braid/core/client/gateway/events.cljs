(ns braid.core.client.gateway.events
  (:require
   [braid.core.client.gateway.forms.create-group.events :as create-group]
   [braid.core.client.gateway.forms.join-group.events :as join-group]
   [braid.core.client.gateway.forms.user-auth.events :as user-auth]
   [braid.core.client.gateway.fx]
   [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]))

(reg-event-fx
  ::initialize
  (fn [{state :db} [_ action]]
    {:db (-> state
             (assoc :action action)
             (assoc :action-disabled? true))
     :dispatch [::handle-action]}))

(reg-event-fx
  ::handle-action
  (fn [{state :db} _]
    (case (state :action)
      :create-group
      {:dispatch [::create-group/initialize]}

      :log-in
      {:dispatch [::user-auth/initialize :log-in]}

      :request-password-reset
      {:dispatch [::user-auth/initialize :request-password-reset]}

      :join-group
      {:dispatch [::join-group/initialize]})))

(reg-event-fx
  ::change-user-status
  (fn [{state :db} [_ logged-in?]]
    (merge
      {:db (assoc state :action-disabled? (not logged-in?))}
      (cond
        (and (= :log-in (state :action)) logged-in?)
        {:dispatch [:start-socket]}

        (and (= :join-group (state :action)) logged-in?)
        {:dispatch [::join-group/remote-join-group]}

        :else {}))))
