(ns braid.core.client.state.fx.dispatch-debounce)

(defonce timeouts
  (atom {}))

(defn dispatch
  [[id event-vec n]]
  (js/clearTimeout (@timeouts id))
  (swap! timeouts assoc id
         (js/setTimeout (fn []
                          (dispatch event-vec)
                          (swap! timeouts dissoc id))
                        n)))

(defn stop [id]
  (js/clearTimeout (@timeouts id))
  (swap! timeouts dissoc id))
