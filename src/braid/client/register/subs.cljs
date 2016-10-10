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
      (get-in state [:fields field :focused?]) :focused
      (< 0 (get-in state [:fields field :validations-left])) :loading
      (string/blank? (get-in state [:fields field :value])) :blank
      (not (empty? (get-in state [:fields field :errors]))) :invalid
      :else :valid)))


