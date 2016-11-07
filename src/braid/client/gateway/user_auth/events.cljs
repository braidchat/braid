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
                           :checking? true
                           :register? true
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
    ; TODO ajax request to log-out
    {:dispatch-n [[:gateway.user-auth/set-user nil]
                  [:gateway.user-auth/set-user-register? false]]}))


(defn blank->nil [s]
  (when-not (string/blank? s) s))

(reg-event-fx
  :gateway.user-auth/set-user-register?
  (fn [{state :db} [_ bool]]
    {:db (-> state
             (assoc-in [:user-auth :register?] bool))}))

(reg-event-fx
  :gateway.user-auth/fake-remote-auth
  (fn [{state :db} _]
    (js/setTimeout (fn []
                     (dispatch
                       [:gateway.user-auth/set-user
                        {:id "1234"
                         :nickname "rafd"
                         :email "rafal.dittwald@gmail.com"
                         :avatar "https://en.gravatar.com/userimage/612305/740d38e04f1c21f1fb27e76b5f63852a.jpeg"}]))
      1000)
    {:db (assoc-in state [:user-auth :checking?] true)}))

(reg-event-fx
  :gateway.user-auth/remote-check-auth
  (fn [{state :db} _]
    ; TODO ajax request to check auth status
    (dispatch [:gateway.user-auth/fake-remote-auth])
    {}))

(reg-event-fx
  :gateway.user-auth/remote-oauth
  (fn [{state :db} [_ provider]]
    ; TODO kick off oauth process
    (dispatch [:gateway.user-auth/fake-remote-auth])
    {:db (assoc-in state [:user-auth :oauth-provider] provider)}))

(reg-event-fx
  :gateway.user-auth/remote-log-in
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:gateway.user-auth/fake-remote-auth])))

(reg-event-fx
  :gateway.user-auth/remote-register
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:gateway.user-auth/fake-remote-auth])))

