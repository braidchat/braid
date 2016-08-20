(ns braid.client.calls.remote-handlers
  (:require
    [braid.client.sync :as sync]
    [braid.client.webrtc :as rtc]
    [braid.client.dispatcher :refer [dispatch!]]))

(defmethod sync/event-handler :braid.client/receive-new-call
  [[_ call]]
  (dispatch! :calls/receive-new-call call))

(defmethod sync/event-handler :braid.client/receive-new-call-status
  [[_ [call status]]]
  (dispatch! :calls/set-receiver-call-status [call status]))

(defmethod sync/event-handler :braid.client/receive-protocol-signal
  [[_ signal]]
  (rtc/receive-protocol-signal signal))
