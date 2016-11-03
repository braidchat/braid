(ns braid.client.register.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [clojure.string :as string]))

(reg-sub
  :register/field-valid?
  (fn [state [_ field]]
    (empty? (get-in state [:fields field :errors]))))

(reg-sub
  :register/field-value
  (fn [state [_ field]]
    (get-in state [:fields field :value])))

(reg-sub
  :register/field-errors
  (fn [state [_ field]]
    (get-in state [:fields field :errors])))

(reg-sub
  :register/field-status
  (fn [state [_ field]]
    (cond
      (get-in state [:fields field :typing?]) :typing
      (get-in state [:fields field :untouched?]) :untouched
      (not (empty? (get-in state [:fields field :errors]))) :invalid
      (< 0 (get-in state [:fields field :validations-left])) :loading
      :else :valid)))

(reg-sub
  :register/sending?
  (fn [state _]
    (state :sending?)))

(reg-sub
  :register/action-mode
  (fn [state _]
    (state :action-mode)))

; USER AUTH SECTION

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
