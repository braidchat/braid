(ns braid.client.gateway.forms.user-auth.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.helpers :as helpers]
    [braid.client.gateway.forms.user-auth.validations :refer [validations]]))

(reg-event-fx
  ::initialize
  (fn [{state :db} [_ mode]]
    {:db (-> state
             (assoc :csrf-token nil)
             (assoc
               :user-auth {:user nil
                           :error nil
                           :checking? true
                           :mode mode ; :register :log-in , :request-password-reset
                           :should-validate? false
                           :oauth-provider nil
                           :validations validations
                           :fields (helpers/init-fields validations)}))
     :dispatch-n [[::remote-check-auth]
                  [::validate-all]]}))

(reg-event-fx
  ::set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
           (assoc-in [:user-auth :user] data)
           (assoc-in [:user-auth :checking?] false)
           (assoc-in [:user-auth :oauth-provider] nil))
     :dispatch (if data
                 [:braid.client.gateway.events/change-user-status true]
                 [:braid.client.gateway.events/change-user-status false])}))

(reg-event-fx
  ::set-csrf-token
  (fn [{state :db} [_ token]]
    {:db (assoc state :csrf-token token)}))

(reg-event-fx
  ::switch-account
  (fn [{state :db} _]
    {:dispatch-n [[::update-value :email ""]
                  [::update-value :password ""]
                  [::set-mode :log-in]
                  [::remote-log-out]]}))


(defn blank->nil [s]
  (when-not (string/blank? s) s))

(defn clear-error [state]
  (assoc-in state [:user-auth :error] nil))

(reg-event-fx
  ::set-mode
  (fn [{state :db} [_ mode]]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :should-validate?] false)
             (assoc-in [:user-auth :mode] mode))}))

(reg-event-fx
  ::set-error
  (fn [{state :db} [_ k]]
    {:db (-> state
             (assoc-in [:user-auth :error] k))}))

; REMOTE

(reg-event-fx
  ::remote-check-auth
  (fn [{state :db} _]
    {:db (assoc-in state [:user-auth :checking?] true)
     :edn-xhr {:uri "/session"
               :method :get
               :on-complete (fn [{:keys [user csrf-token]}]
                              (dispatch [::set-user user])
                              (dispatch [::set-csrf-token csrf-token]))
               :on-error (fn [_]
                           (dispatch [::set-user nil]))}}))

(reg-event-fx
  ::remote-log-out
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/session"
               :method :delete
               :headers {"x-csrf-token" (state :csrf-token)}
               :on-complete (fn [_]
                              (dispatch [::set-user nil]))
               :on-error (fn [_]
                           (dispatch [::set-user nil]))}}))

(defn message-event-handler [e]
  (dispatch [::remote-oauth
             (.. e -data -code)
             (.. e -data -state)]))

(defn init-message-listener! []
  ; using a named function, b/c an anonymous function would  get registered multiple times
  (js/window.addEventListener "message" message-event-handler))

(reg-event-fx
  ::open-oauth-window
  (fn [{state :db} [_ provider]]
    (init-message-listener!)
    (case provider
      :github
      (.open js/window
             "/gateway/oauth/github/auth"
             "GitHub OAuth"
             "width=300,height=400"))
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :oauth-provider] provider))}))

(reg-event-fx
  ::remote-oauth
  (fn [_ [_ code state]]
    {:edn-xhr {:uri "/session/oauth/github"
               :method :put
               :params {:code code
                        :state state}
               :on-complete
               (fn [user]
                 (dispatch [::remote-check-auth]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::set-error k]))
                 (dispatch [::set-user nil]))}}))

(reg-event-fx
  ::remote-log-in
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/session"
               :method :put
               :params {:email (get-in state [:user-auth :fields :email :value])
                        :password (get-in state [:user-auth :fields :password :value])}
               :on-complete (fn [user]
                              (dispatch [::remote-check-auth]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::set-error k]))
                 (dispatch [::set-user nil]))}}))

(reg-event-fx
  ::remote-register
  (fn [{state :db} _]
    {:db (-> state
             clear-error
             (assoc-in [:user-auth :checking?] true))
     :edn-xhr {:uri "/users"
               :method :put
               :params {:email (get-in state [:user-auth :fields :email :value])
                        :password (get-in state [:user-auth :fields :new-password :value])}
               :on-complete (fn [user]
                              (dispatch [::remote-check-auth]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::set-error k]))
                 (dispatch [::set-user nil]))}}))

(reg-event-fx
  ::remote-request-password-reset
  (fn [{state :db} _]
    {:edn-xhr {:uri "/request-reset"
               :method :post
               :params {:email (get-in state [:user-auth :fields :email :value])}
               :on-complete
               (fn [_]
                 (dispatch [::set-error :password-reset-email-sent]))
               :on-error
               (fn [error]
                 (when-let [k (get-in error [:response :error])]
                   (dispatch [::set-error k])))}}))

(helpers/reg-form-event-fxs :braid.client.gateway.forms.user-auth.events :user-auth)
