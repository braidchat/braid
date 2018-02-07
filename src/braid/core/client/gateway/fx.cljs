(ns braid.core.client.gateway.fx
  (:require
   [braid.core.client.xhr :as xhr]
   [re-frame.core :refer [dispatch reg-fx]]))

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
