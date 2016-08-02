(ns braid.client.state.handler.core)

(defmulti handler (fn [state [event data]] event))
