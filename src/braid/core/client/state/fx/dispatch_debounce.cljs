(ns braid.core.client.state.fx.dispatch-debounce
  (:require
    [re-frame.core :as re-frame]))

(defonce timeouts
  (atom {}))

(defn dispatch
  [[id event-vec n]]
  (js/clearTimeout (@timeouts id))
  (swap! timeouts assoc id
         (js/setTimeout (fn []
                          (re-frame/dispatch event-vec)
                          (swap! timeouts dissoc id))
                        n)))

(defn stop [id]
  (js/clearTimeout (@timeouts id))
  (swap! timeouts dissoc id))
