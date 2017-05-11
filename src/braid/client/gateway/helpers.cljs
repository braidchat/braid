(ns braid.client.gateway.helpers
  (:require
    [re-frame.core :refer [reg-event-fx reg-sub dispatch]]
    [braid.client.gateway.validations :refer [validations]]))

(defn with-ns [ns n]
  (keyword (name ns) (name n)))

; events

(defn reg-form-event-fxs [ns]

  (reg-event-fx
    (with-ns ns :blur)
    (fn [{state :db} [_ field]]
      {:db (assoc-in state [:fields field :untouched?] false)
       ; use dispatch-debounce
       ; to cancel possible other identical debounced dispatch
       :dispatch-debounce [field [(with-ns ns :stop-typing) field] 0]}))

  (reg-event-fx
    (with-ns ns :clear-errors)
    (fn [{state :db} [_ field]]
      {:db (assoc-in state [:fields field :errors] [])}))

  (reg-event-fx
    (with-ns ns :stop-typing)
    (fn [{state :db} [_ field]]
      (when (get-in state [:fields field :typing?])
        {:db (assoc-in state [:fields field :typing?] false)
         :dispatch [(with-ns ns :validate-field) field]})))

  (reg-event-fx
    (with-ns ns :update-value)
    (fn [{state :db} [_ field value]]
      {:db (-> state
               (assoc-in [:fields field :value] value)
               (assoc-in [:fields field :typing?] true)
               (assoc-in [:fields field :untouched?] false))
       :dispatch [(with-ns ns :clear-errors) field]
       :dispatch-debounce [field [(with-ns ns :stop-typing) field] 500]}))

  (reg-event-fx
    (with-ns ns :update-field-status)
    (fn [{state :db} [_ field error]]
      {:db (if error
             (-> state
                 (update-in [:fields field :errors] conj error)
                 (update-in [:fields field :validations-left] dec))
             (-> state
                 (update-in [:fields field :validations-left] dec)))}))

  (reg-event-fx
    (with-ns ns :validate)
    (fn [_ [_ [field field-value]]]
      (doseq [validator-fn (validations field)]
        (validator-fn
          field-value
          (fn [error]
            (dispatch [(with-ns ns :update-field-status) field error]))))
      {}))

  (reg-event-fx
    (with-ns ns :validate-field)
    (fn [{state :db} [_ field]]
      (let [validator-fns (validations field)
            field-value (get-in state [:fields field :value])]
        {:db (assoc-in state [:fields field :validations-left] (count validator-fns))
         :dispatch [(with-ns ns :validate) [field field-value]]})))

  (reg-event-fx
    (with-ns ns :validate-all)
    (fn [{state :db} _]
      {:dispatch-n (for [field (keys validations)]
                     [(with-ns ns :validate-field) field])}))

  (defn- touch-fields
    [state fields-to-touch]
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
    (with-ns ns :submit-form)
    (fn [{state :db} [_ {:keys [validate-fields
                                dispatch-when-valid]}]]
      (if-let [all-valid? (->> validate-fields
                               (map (fn [field]
                                      (empty? (get-in state [:fields field :errors]))))
                               (every? true?))]
        {:dispatch dispatch-when-valid}
        {:db (-> state
                 (touch-fields validate-fields)
                 (assoc-in [:should-validate?] true))}))))

; subs

(defn reg-form-subs [ns]

  (reg-sub
    (with-ns ns :field-value)
    (fn [state [_ field]]
      (get-in state [:fields field :value])))

  (reg-sub
    (with-ns ns :field-errors)
    (fn [state [_ field]]
      (get-in state [:fields field :errors])))

  (reg-sub
    (with-ns ns :field-status)
    (fn [state [_ field]]
      (cond
        (not (get-in state [:should-validate?])) :skip
        (get-in state [:fields field :typing?]) :typing
        (get-in state [:fields field :untouched?]) :untouched
        (not (empty? (get-in state [:fields field :errors]))) :invalid
        (< 0 (get-in state [:fields field :validations-left])) :loading
        :else :valid)))

  (reg-sub
    (with-ns ns :fields-valid?)
    (fn [state [_ fields]]
      (->> fields
           (map (fn [field]
                  (and
                    (empty? (get-in state [:fields field :errors]))
                    (= 0 (get-in state [:fields field :validations-left]))
                    (not (get-in state [:fields field :typing?])))))
           (every? true?)))))

