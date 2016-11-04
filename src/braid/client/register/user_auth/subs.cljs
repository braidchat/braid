(ns braid.client.register.user-auth.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :register.user/user
  (fn [state _]
    (get-in state [:user-auth :user])))

(reg-sub
  :register.user/user-auth-mode
  (fn [state _]
    (cond
      (get-in state [:user-auth :checking?]) :checking
      (get-in state [:user-auth :user]) :authed
      (get-in state [:user-auth :oauth-provider]) :oauth-in-progress
      (get-in state [:user-auth :register?]) :register
      :else :log-in)))

(reg-sub
  :register.user/oauth-provider
  (fn [state _]
    (get-in state [:user-auth :oauth-provider])))
