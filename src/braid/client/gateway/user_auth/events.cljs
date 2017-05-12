(ns braid.client.gateway.user-auth.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.helpers :as helpers]
    [braid.client.gateway.user-auth.validations :refer [validations]]))

(reg-event-fx
  :gateway.user-auth/initialize
  (fn [{state :db} [_ mode]]
    {:db (-> state
             (assoc :csrf-token nil)
             (assoc
               :user-auth {:user nil
                           :error nil
                           :checking? true
                           :mode mode ; :register :log-in , :reset-password
                           :should-validate? false
                           :oauth-provider nil
                           :validations validations
                           :fields (helpers/init-fields validations)}))
     :dispatch-n [[:gateway.user-auth/remote-check-auth]
                  [:gateway.user-auth/validate-all]]}))

(reg-event-fx
  :gateway.user-auth/set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
           (assoc-in [:user-auth :user] data)
           (assoc-in [:user-auth :checking?] false)
           (assoc-in [:user-auth :oauth-provider] nil))}))

(reg-event-fx
  :gateway.user-auth/set-csrf-token
  (fn [{state :db} [_ token]]
    {:db (assoc state :csrf-token token)}))

(reg-event-fx
  :gateway.user-auth/switch-account
  (fn [{state :db} _]
    {:dispatch-n [[:gateway.user-auth/update-value :gateway.user-auth/email ""]
                  [:gateway.user-auth/update-value :gateway.user-auth/password ""]
                  [:gateway.user-auth/set-mode :log-in]
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
             (assoc-in [:user-auth :should-validate?] false)
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
               :on-complete (fn [{:keys [user csrf-token]}]
                              (dispatch [:gateway.user-auth/set-user user])
                              (dispatch [:gateway.user-auth/set-csrf-token csrf-token]))
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
               :headers {"x-csrf-token" (state :csrf-token)}
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
               :params {:email (get-in state [:user-auth :fields :gateway.user-auth/email :value])
                        :password (get-in state [:user-auth :fields :gateway.user-auth/password :value])}
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
               :params {:email (get-in state [:user-auth :fields :gateway.user-auth/email :value])
                        :password (get-in state [:user-auth :fields :gateway.user-auth/new-password :value])}
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
               :params {:email (get-in state [:user-auth :fields :gateway.user-auth/email :value])}
               :on-complete
               (fn [_]
                 (dispatch [:gateway.user-auth/set-error :password-reset-email-sent]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [:gateway.user-auth/set-error k]))) }}))

(helpers/reg-form-event-fxs :gateway.user-auth :user-auth)
