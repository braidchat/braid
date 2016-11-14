(ns braid.client.gateway.user-auth.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]))

(reg-event-fx
  :gateway.user-auth/initialize
  (fn [{state :db} _]
    {:db (-> state
             (assoc
               :user-auth {:user nil
                           :error nil
                           :checking? true
                           :mode :register ; :log-in , :reset-password
                           :oauth-provider nil}))
     :dispatch [:gateway.user-auth/remote-check-auth]}))

(reg-event-fx
  :gateway.user-auth/set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
           (assoc-in [:user-auth :user] data)
           (assoc-in [:user-auth :checking?] false)
           (assoc-in [:user-auth :oauth-provider] nil))}))

(reg-event-fx
  :gateway.user-auth/switch-account
  (fn [{state :db} _]
    {:dispatch-n [[:gateway.user-auth/mode :log-in]
                  [:gateway.user-auth/remote-log-out]]}))


(defn blank->nil [s]
  (when-not (string/blank? s) s))

(defn clear-error [state]
  (assoc-in state [:user-auth :error] nil))

(reg-event-fx
  :gateway.user-auth/set-mode
  (fn [{state :db} [_ mode]]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :mode] mode))}))

(reg-event-fx
  :gateway.user-auth/set-error
  (fn [{state :db} [_ k]]
    {:db (-> state
             (assoc-in [:user-auth :error] k))}))

; REMOTE

(reg-event-fx
  :gateway.user-auth/remote-check-auth
  (fn [{state :db} _]
    {:db (assoc-in state [:user-auth :checking?] true)
     :edn-xhr {:uri "/session"
               :method :get
               :on-complete (fn [user]
                              (dispatch [:gateway.user-auth/set-user user]))
               :on-error (fn [_]
                           (dispatch [:gateway.user-auth/set-user nil]))}}))

(reg-event-fx
  :gateway.user-auth/remote-log-out
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/session"
               :method :delete
               :on-complete (fn [_]
                              (dispatch [:gateway.user-auth/set-user nil]))
               :on-error (fn [_]
                           (dispatch [:gateway.user-auth/set-user nil]))}}))

(reg-event-fx
  :gateway.user-auth/remote-oauth
  (fn [{state :db} [_ provider]]
    ; TODO kick off oauth process
    (dispatch [:gateway.user-auth/fake-remote-auth])
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :oauth-provider] provider))}))

(reg-event-fx
  :gateway.user-auth/remote-log-in
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/session"
               :method :put
               :params {:email (get-in state [:fields :gateway.user-auth/email :value])
                        :password (get-in state [:fields :gateway.user-auth/password :value])}
               :on-complete (fn [user]
                              (dispatch [:gateway.user-auth/remote-check-auth]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:gateway.user-auth/set-error k]))
                 (dispatch [:gateway.user-auth/set-user nil]))}}))

(reg-event-fx
  :gateway.user-auth/remote-register
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/users"
               :method :put
               :params {:email (get-in state [:fields :gateway.user-auth/email :value])
                        :password (get-in state [:fields :gateway.user-auth/password :value])}
               :on-complete (fn [user]
                              (dispatch [:gateway.user-auth/remote-check-auth]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:gateway.user-auth/set-error k]))
                 (dispatch [:gateway.user-auth/set-user nil]))}}))

(reg-event-fx
  :gateway.user-auth/remote-request-password-reset
  (fn [{state :db} _]
    {:edn-xhr {:uri "/request-reset"
               :method :post
               :params {:email (get-in state [:fields :gateway.user-auth/email :value])}
               :on-complete
               (fn [_]
                 (dispatch [:gateway.user-auth/set-error :password-reset-email-sent]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:gateway.user-auth/set-error k]))) }}))
