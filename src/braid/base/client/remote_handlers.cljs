(ns braid.base.client.remote-handlers
  (:require
    [braid.core.hooks :as hooks]
    [braid.base.client.socket :as socket]
    [taoensso.timbre :as timbre :refer-macros [errorf]]))

(defonce incoming-socket-message-handlers
  (hooks/register! (atom {}) {keyword? fn?}))

(defmethod socket/event-handler :default
  [[id data]]
  (if-let [handler (@incoming-socket-message-handlers id)]
    (handler id data)
    (errorf "No socket message handler for id: %s" id)))
