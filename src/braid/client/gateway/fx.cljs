(ns braid.client.gateway.fx
  (:require
    [re-frame.core :refer [dispatch reg-fx]]
    [braid.client.gateway.validations :refer [validations]]))

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

(reg-fx :validate
  (fn [[field field-value]]
    (doseq [validator-fn (validations field)]
      (validator-fn
        field-value
        (fn [error]
          (dispatch [:gateway/update-field-status field error]))))))
