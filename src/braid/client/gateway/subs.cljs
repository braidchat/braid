(ns braid.client.gateway.subs
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-sub]]
    [braid.client.gateway.user-auth.subs]
    [braid.client.gateway.create-group.subs]))

(reg-sub
  :gateway/field-valid?
  (fn [state [_ field]]
    (empty? (get-in state [:fields field :errors]))))

(reg-sub
  :gateway/field-value
  (fn [state [_ field]]
    (get-in state [:fields field :value])))

(reg-sub
  :gateway/field-errors
  (fn [state [_ field]]
    (get-in state [:fields field :errors])))

(reg-sub
  :gateway/field-status
  (fn [state [_ field]]
    (cond
      (get-in state [:fields field :typing?]) :typing
      (get-in state [:fields field :untouched?]) :untouched
      (not (empty? (get-in state [:fields field :errors]))) :invalid
      (< 0 (get-in state [:fields field :validations-left])) :loading
      :else :valid)))

(reg-sub
  :gateway.action.create-group/sending?
  (fn [state _]
    (get-in state [:action :sending?])))

(reg-sub
  :gateway.action/mode
  (fn [state _]
    (get-in state [:action :mode])))

(reg-sub
  :gateway/fields-valid?
  (fn [state [_ fields]]
    (->> fields
         (map (fn [field]
                (empty? (get-in state [:fields field :errors]))))
         (every? true?))))
