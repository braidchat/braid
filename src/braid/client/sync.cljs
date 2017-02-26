(ns braid.client.sync
  (:require [re-frame.core :refer [dispatch]]
            [taoensso.sente  :as sente :refer [cb-success?]]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as timbre :refer-macros [debugf]]
            [goog.string :as gstring]
            [braid.client.store :as store]
            [goog.string.format]))

; Change to :debug to get detailed info in dev
(timbre/set-level! :info)

(defn make-socket! []
  (let [domain (aget js/window "api_domain")
        packer (sente-transit/get-transit-packer
                 :json
                 {}
                 ; Need to decode UUIDs as clojurescript uuids, not
                 ; cognitect.transit or schema gets upset
                 {:handlers {"u" cljs.core/uuid}})
        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk"
                                    {:host domain
                                     :ws-kalive-ms 5000
                                     :path "/chsk"
                                     :packer packer})]
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
  [{[old-state new-state] :?data}]

  (if (new-state :open?)
    (if (= :taoensso.sente/nil-uid (new-state :uid))
      ; reconnected, but session has expired
      ; user needs to log in again
      (dispatch [:core/websocket-needs-auth])
      (dispatch [:core/websocket-connected]))
    (dispatch [:core/websocket-disconnected]))

  (when-let [next-reconnect (new-state :udt-next-reconnect)]
    (dispatch [:core/websocket-update-next-reconnect next-reconnect]))

  (event-handler [:socket/connected new-state]))

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

(defn disconnect! []
  (sente/chsk-disconnect! chsk))

(defn reconnect! []
  (sente/chsk-reconnect! chsk))

(defn connect! []
  (if-not chsk
    (do
      (make-socket!)
      (start-router!))
    (reconnect!)))
