(ns braid.client.calls.subscriptions
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.state.subscription :refer [subscription]]))

(defmethod subscription :calls
  [state _]
  (reaction (vals (@state :calls))))

(defmethod subscription :call-status
  [state [_ call]]
  (reaction (get-in @state [:calls (call :id) :status])))

(defmethod subscription :new-call
  [state _]
  (reaction (->> (@state :calls)
                 vals
                 (filter (fn [c] (not= :archived (c :status))))
                 (sort-by :created-at)
                 first)))

(defmethod subscription :current-user-is-caller?
  [state [_ caller-id]]
  (reaction (= @(subscription state [:user-id]) caller-id)))

(defmethod subscription :correct-nickname
  [state [_ call]]
  (let [is-caller? (reaction @(subscription state [:current-user-is-caller? (call :caller-id)]))
        caller-nickname (reaction @(subscription state [:nickname (call :caller-id)]))
        callee-nickname (reaction @(subscription state [:nickname (call :callee-id)]))]
    (reaction (if @is-caller? @callee-nickname @caller-nickname))))
