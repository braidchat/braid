(ns braid.client.calls.handlers
  (:require [braid.client.webrtc :as rtc]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.sync :as sync]
            [braid.client.schema :as schema]
            [braid.client.calls.helpers :as helpers]
            [braid.client.state.handler.core :refer [handler]]))

(defmethod handler :calls/start-new-call [state [_ data]]
  (rtc/get-ice-servers
    (fn [servers]
      (rtc/create-connections servers)
      (let [call (schema/make-call data)]
        (sync/chsk-send! [:braid.server/make-new-call call])
        (dispatch! :calls/add-new-call call))))
  state)

(defmethod handler :calls/receive-new-call [state [_ call]]
  (rtc/get-ice-servers
    (fn [servers]
      (rtc/create-connections servers)
      (dispatch! :calls/add-new-call call)))
  state)

(defmethod handler :calls/add-new-call [state [_ call]]
  (helpers/add-call state call))

(defmethod handler :calls/set-requester-call-status [state [_ [call status]]]
  (when (= status :accepted)
    (rtc/set-stream))
  (sync/chsk-send! [:braid.server/change-call-status {:call call :status status}])
  (helpers/set-call-status state (call :id) status))

(defmethod handler :calls/set-receiver-call-status [state [_ [call status]]]
  (when (= status :accepted)
    (rtc/set-stream))
  (helpers/set-call-status state (call :id) status))
