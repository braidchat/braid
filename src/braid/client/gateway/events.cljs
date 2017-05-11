(ns braid.client.gateway.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.gateway.fx]
    [braid.client.gateway.user-auth.events]
    [braid.client.gateway.create-group.events]
    [braid.client.gateway.validations :refer [validations]])
  (:import
    [goog Uri]))

(defn get-url-param [param]
  (.getParameterValue (.parse Uri js/window.location) (name param)))

(reg-event-fx
  :gateway/initialize
  (fn [_ _]
    {:db {:action (keyword (aget js/window "gateway_action"))
          :fields (reduce (fn [memo field]
                            (let [prefilled-value (get-url-param field)]
                              (assoc memo field
                                {:value (or prefilled-value "")
                                 :typing? false
                                 :untouched? (if prefilled-value
                                               false
                                               true)
                                 :validations-left 0
                                 :errors []})))
                          {}
                          (keys validations))}
     :dispatch-n [[:gateway/initialize-action]]}))

(reg-event-fx
  :gateway/initialize-action
  (fn [{state :db} _]
    (case (state :action)
      :create-group
      {:dispatch-n [[:gateway.user-auth/initialize :register]
                    [:gateway.action.create-group/initialize]]}

      :log-in
      {:dispatch-n [[:gateway.user-auth/initialize :log-in]]})))
