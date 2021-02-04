(ns braid.base.server.cqrs
  (:require
    [tada.events.core :as tada]
    [braid.core.hooks :as hooks]
    [taoensso.timbre :as timbre]))

(defn dispatch [command-id args]
  (tada/do! command-id args))

(defonce commands (hooks/register! (atom [])))

(defn ->ws-handler [command]
  (fn [{:keys [user-id ?data ?reply-fn]}]
    (timbre/debug "Command:" (:id command) ?data)
    (try
      (dispatch (:id command)
                (assoc ?data
                  :user-id user-id))
      (?reply-fn :braid/ok)
      (catch Exception e
        (timbre/warn "Command Error:" e)
        (?reply-fn {:cqrs/error {:message (.getMessage e)
                                 :data (ex-data e)}})))
    ;; returning {} here, b/c our socket-message-handlers
    ;; expect a map of cofx
    {}))

(defn update-registry! []
  (tada/register! @commands))
