(ns braid.client.register.user-auth.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :register.user/user
  (fn [state _]
    (get-in state [:user-auth-section :user])))

(reg-sub
  :register.user/user-auth-section-mode
  (fn [state _]
    (cond
      (get-in state [:user-auth-section :checking?]) :checking
      (get-in state [:user-auth-section :user]) :authed
      (get-in state [:user-auth-section :oauth-provider]) :oauth-in-progress
      (get-in state [:user-auth-section :register?]) :register
      :else :log-in)))

(reg-sub
  :register.user/oauth-provider
  (fn [state _]
    (get-in state [:user-auth-section :oauth-provider])))
