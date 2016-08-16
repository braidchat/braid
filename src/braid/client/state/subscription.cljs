(ns braid.client.state.subscription)

(defmulti subscription
  "Create a reaction for the particular type of information.
  Do not call directly, should be invoked by `subscribe`"
  {:arglists '([state [sub-name args]])}
  (fn [_ [sub-name _]] sub-name))
