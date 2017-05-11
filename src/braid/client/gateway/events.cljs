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

(reg-event-fx
  :gateway/blur
  (fn [{state :db} [_ field]]
    {:db (assoc-in state [:fields field :untouched?] false)
     ; use dispatch-debounce
     ; to cancel possible other identical debounced dispatch
     :dispatch-debounce [field [:gateway/stop-typing field] 0]}))

(reg-event-db
  :gateway/clear-errors
  (fn [state [_ field]]
    (assoc-in state [:fields field :errors] [])))

(reg-event-fx
  :gateway/stop-typing
  (fn [{state :db} [_ field]]
    (when (get-in state [:fields field :typing?])
      {:db (assoc-in state [:fields field :typing?] false)
       :dispatch [:gateway/validate-field field]})))

(reg-event-fx
  :gateway/update-value
  (fn [{state :db} [_ field value]]
    {:db (-> state
             (assoc-in [:fields field :value] value)
             (assoc-in [:fields field :typing?] true)
             (assoc-in [:fields field :untouched?] false))
     :dispatch [:gateway/clear-errors field]
     :dispatch-debounce [field [:gateway/stop-typing field] 500]}))

(reg-event-db
  :gateway/update-field-status
  (fn [state [_ field error]]
    (if error
      (-> state
          (update-in [:fields field :errors] conj error)
          (update-in [:fields field :validations-left] dec))
      (-> state
          (update-in [:fields field :validations-left] dec)))))

(reg-event-fx
  :gateway/validate-field
  (fn [{state :db} [_ field]]
    (let [validator-fns (validations field)
          field-value (get-in state [:fields field :value])]
      {:db (assoc-in state [:fields field :validations-left] (count validator-fns))
       :validate [field field-value]})))

(reg-event-fx
  :gateway/validate-all
  (fn [{state :db} _]
    {:dispatch-n (for [field (keys validations)]
                   [:gateway/validate-field field])}))

(defn touch-fields [state fields-to-touch]
  (update state :fields
          (fn [fields]
            (reduce (fn [memo [field-id v]]
                      (assoc memo field-id
                        (if (contains? (set fields-to-touch) field-id)
                          (assoc v :untouched? false)
                          v)))
                    {}
                    fields))))

(reg-event-fx
  :gateway/submit-form
  (fn [{state :db} [_ {:keys [validate-fields
                              dispatch-when-valid]}]]
    (if-let [all-valid? (->> validate-fields
                             (map (fn [field]
                                    (empty? (get-in state [:fields field :errors]))))
                             (every? true?))]
      {:dispatch dispatch-when-valid}
      {:db (touch-fields state validate-fields)})))

