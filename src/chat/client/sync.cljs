(ns chat.client.sync
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! put! chan alts!]]
            [taoensso.sente  :as sente :refer [cb-success?]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn debugf [s & args]
  (js/console.log (apply gstring/format s args)))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

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
    (debugf "Channel socket state change: %s" ?data))
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
