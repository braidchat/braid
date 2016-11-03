(ns braid.client.register.user-auth.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]))

(reg-event-fx
  :register.user/set-user
  (fn [{state :db} [_ data]]
    {:db (-> state
           (assoc-in [:user-auth-section :user] data)
           (assoc-in [:user-auth-section :checking?] false)
           (assoc-in [:user-auth-section :oauth-provider] nil))}))

(reg-event-fx
  :register.user/switch-account
  (fn [{state :db} _]
    ; TODO ajax request to log-out
    {:dispatch-n [[:register.user/set-user nil]
                  [:register.user/set-user-register? false]]}))

(reg-event-fx
  :register.user/set-user-register?
  (fn [{state :db} [_ bool]]
    {:db (assoc-in state [:user-auth-section :register?] bool)}))

(reg-event-fx
  :register.user/fake-remote-auth
  (fn [{state :db} _]
    (js/setTimeout (fn []
                     (dispatch
                       [:register.user/set-user
                        {:id "1234"
                         :nickname "rafd"
                         :email "rafal.dittwald@gmail.com"
                         :avatar "https://en.gravatar.com/userimage/612305/740d38e04f1c21f1fb27e76b5f63852a.jpeg"}]))
      1000)
    {:db (assoc-in state [:user-auth-section :checking?] true)}))

(reg-event-fx
  :register.user/remote-check-auth
  (fn [{state :db} _]
    ; TODO ajax request to check auth status
    (dispatch [:register.user/fake-remote-auth])
    {}))

(reg-event-fx
  :register.user/remote-oauth
  (fn [{state :db} [_ provider]]
    ; TODO kick off oauth process
    (dispatch [:register.user/fake-remote-auth])
    {:db (assoc-in state [:user-auth-section :oauth-provider] provider)}))

(reg-event-fx
  :register.user/remote-log-in
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:register.user/fake-remote-auth])))

(reg-event-fx
  :register.user/remote-register
  (fn [{state :db} _]
    ; TODO kick off login process
    (dispatch [:register.user/fake-remote-auth])))
