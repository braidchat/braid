(ns braid.client.gateway.user-auth.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :gateway.user-auth/user
  (fn [state _]
    (get-in state [:user-auth :user])))

(reg-sub
  :gateway.user-auth/user-auth-mode
  (fn [state _]
    (cond
      (get-in state [:user-auth :checking?]) :checking
      (get-in state [:user-auth :user]) :authed
      (get-in state [:user-auth :oauth-provider]) :oauth-in-progress
      (get-in state [:user-auth :register?]) :register
      :else :log-in)))

(reg-sub
  :gateway.user-auth/oauth-provider
  (fn [state _]
    (get-in state [:user-auth :oauth-provider])))

(reg-sub
  :gateway.user-auth/error
  (fn [state _]
    (get-in state [:user-auth :error])))
