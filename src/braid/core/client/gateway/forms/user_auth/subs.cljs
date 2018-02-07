(ns braid.core.client.gateway.forms.user-auth.subs
  (:require
   [braid.core.client.gateway.helpers :as helpers]
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::user
  (fn [state _]
    (get-in state [:user-auth :user])))

(reg-sub
  ::user-auth-mode
  (fn [state _]
    (cond
      (nil? (get-in state [:user-auth])) :checking
      (get-in state [:user-auth :checking?]) :checking
      (get-in state [:user-auth :user]) :authed
      (get-in state [:user-auth :oauth-provider]) :oauth-in-progress
      :else (get-in state [:user-auth :mode]))))

(reg-sub
  ::oauth-provider
  (fn [state _]
    (get-in state [:user-auth :oauth-provider])))

(reg-sub
  ::error
  (fn [state _]
    (get-in state [:user-auth :error])))

(helpers/reg-form-subs :braid.core.client.gateway.forms.user-auth.subs :user-auth)
