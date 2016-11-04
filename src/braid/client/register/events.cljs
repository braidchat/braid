(ns braid.client.register.events
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [braid.client.register.fx]
    [braid.client.register.user-auth.events]
    [braid.client.register.create-group.events]
    [braid.client.register.validations :refer [validations]])
  (:import
    [goog Uri]))

(defn get-url-param [param]
  (.getParameterValue (.parse Uri js/window.location) (name param)))

(def fields
  [:email :name :url :type])

; EVENTS

(reg-event-fx
  :register/initialize
  (fn [{state :db} _]
    {:db (-> state
             (assoc
               :fields (reduce (fn [memo field]
                                 (assoc memo field
                                   {:value (or (get-url-param field) "")
                                    :typing false
                                    :untouched? true
                                    :validations-left 0
                                    :errors []}))
                               {} fields)))
     :dispatch-n [[:validate-all]
                  [:register.user/initialize]
                  [:register.action.create-group/initialize]]}))

(reg-event-fx
  :blur
  (fn [{state :db} [_ field]]
    {:db (assoc-in state [:fields field :untouched?] false)
     ; use dispatch-debounce
     ; to cancel possible other identical debounced dispatch
     :dispatch-debounce [field [:stop-typing field] 0]}))

(reg-event-db
  :clear-errors
  (fn [state [_ field]]
    (assoc-in state [:fields field :errors] [])))

(reg-event-fx
  :stop-typing
  (fn [{state :db} [_ field]]
    (when (get-in state [:fields field :typing?])
      {:db (assoc-in state [:fields field :typing?] false)
       :dispatch [:validate-field field]})))

(reg-event-fx
  :update-value
  (fn [{state :db} [_ field value]]
    {:db (-> state
             (assoc-in [:fields field :value] value)
             (assoc-in [:fields field :typing?] true)
             (assoc-in [:fields field :untouched?] false))
     :dispatch [:clear-errors field]
     :dispatch-debounce [field [:stop-typing field] 500]}))

(reg-event-db
  :update-field-status
  (fn [state [_ field error]]
    (if error
      (-> state
          (update-in [:fields field :errors] conj error)
          (update-in [:fields field :validations-left] dec))
      (-> state
          (update-in [:fields field :validations-left] dec)))))

(reg-event-db
  :touch-all-fields
  (fn [state _]
    (update state :fields (fn [fields]
                            (reduce (fn [memo [k v]]
                                      (assoc memo k (assoc v :untouched? false))) {}  fields)))))

(reg-event-fx
  :validate-field
  (fn [{state :db} [_ field]]
    (let [validator-fns (validations field)
          field-value (get-in state [:fields field :value])]
      {:db (assoc-in state [:fields field :validations-left] (count validator-fns))
       :validate-n [[field field-value]]})))

(reg-event-fx
  :validate-all
  (fn [{state :db} _]
    {:validate-n (for [field fields]
                   [field (get-in state [:fields field :value])])}))


