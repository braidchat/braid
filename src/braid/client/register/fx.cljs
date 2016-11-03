(ns braid.client.register.fx
  (:require
    [re-frame.core :refer [dispatch reg-fx]]
    [braid.client.register.validations :refer [validations]]))

(defonce timeouts
  (atom {}))

(reg-fx :dispatch-debounce
  (fn [[id event-vec n]]
    (js/clearTimeout (@timeouts id))
    (swap! timeouts assoc id
      (js/setTimeout (fn []
                       (dispatch event-vec)
                       (swap! timeouts dissoc id))
        n))))

(reg-fx :stop-debounce
  (fn [id]
    (js/clearTimeout (@timeouts id))
    (swap! timeouts dissoc id)))

(reg-fx :validate-n
  (fn [to-validate]
    (doseq [[field field-value] to-validate]
      (doseq [validator-fn (validations field)]
        (validator-fn
          field-value
          (fn [error]
            (dispatch [:update-field-status field error])))))))
