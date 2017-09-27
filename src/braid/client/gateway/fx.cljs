(ns braid.client.gateway.fx
  (:require
    [re-frame.core :refer [dispatch reg-fx]]
    [braid.client.xhr :as xhr]))

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

(reg-fx :edn-xhr xhr/edn-xhr)
