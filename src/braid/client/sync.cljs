(ns braid.client.sync
  (:require [taoensso.sente  :as sente :refer [cb-success?]]
            [taoensso.timbre :as timbre :refer-macros [debugf]]
            [goog.string :as gstring]
            [braid.client.store :as store]
            [goog.string.format]))

; Change to :debug to get detailed info in dev
(timbre/set-level! :info)

(defn make-socket! []
  (let [domain (aget js/window "api_domain")
        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk"
                                    {:host domain
                                     :path "/chsk"})]
    (def chsk       chsk)
    (def ch-chsk    ch-recv)
    (def chsk-send! send-fn)
    (def chsk-state state)))

(defmulti event-handler (fn [[id _]] id))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event]}]
  (debugf "Unhandled event: %s" event))

(defmethod event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (?data :first-open?)
    (debugf "Channel socket successfully established!")
    (do
      (debugf "Channel socket state change: %s" ?data)
      (if (not (:open? ?data))
        (store/display-error! :disconnected "Disconnected" :warn)
        (store/clear-error! :disconnected))))
  (event-handler [:socket/connected ?data]))

(defmethod event-msg-handler :chsk/recv
  [{:as ev-msg :keys [event ?data]}]
  (debugf "Push event from server: %s" ?data)
  (event-handler ?data))

(defmethod event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))

(def router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn reconnect! []
  (sente/chsk-reconnect! chsk))
