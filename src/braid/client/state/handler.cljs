(ns braid.client.state.handler)

(defmulti handler (fn [state [event data]] event))
