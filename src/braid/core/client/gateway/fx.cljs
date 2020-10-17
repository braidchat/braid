(ns braid.core.client.gateway.fx
  (:require
   [braid.lib.xhr :as xhr]
   [braid.core.client.state.fx.dispatch-debounce :as fx.debounce]
   [re-frame.core :refer [dispatch reg-fx]]))

(reg-fx :dispatch-debounce
        fx.debounce/dispatch)

(reg-fx :stop-debounce
        fx.debounce/stop)

(reg-fx :edn-xhr xhr/edn-xhr)
