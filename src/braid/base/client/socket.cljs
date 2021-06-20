(ns braid.base.client.socket
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [braid.lib.xhr :as xhr]
    [cljs.core.async :as async]
    [clojure.string :as string]
    [goog.object :as o]
    [goog.string.format]
    [re-frame.core :refer [dispatch]]
    [taoensso.sente :as sente]
    [taoensso.sente.packers.transit :as sente-transit]
    [taoensso.timbre :as timbre :refer-macros [debugf]]))

; Change to :debug to get detailed info in dev
(timbre/set-level! :info)

(defn make-socket! [csrf-token]
  (let [packer (sente-transit/get-transit-packer
                 :json
                 {}
                 ; Need to decode UUIDs as clojurescript uuids, not
                 ; cognitect.transit or schema gets upset
                 {:handlers {"u" cljs.core/uuid}})
        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/api/chsk" csrf-token
                                    {:ws-kalive-ms 5000
                                     :path "/api/chsk"
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
    ;; TODO: seperate events for
    (cond
      (= :taoensso.sente/nil-uid (new-state :uid))
      ; reconnected, but session has expired
      ; user needs to log in again
      (dispatch [:core/websocket-needs-auth!])

      (and (string? (new-state :uid))
           (string/starts-with? (new-state :uid) "FAKE"))
      (dispatch [:core/websocket-anon-connected!])

      :else
      (dispatch [:core/websocket-connected!]))
    (dispatch [:core/websocket-disconnected!]))

  (when-let [next-reconnect (new-state :udt-next-reconnect)]
    (dispatch [:core/websocket-update-next-reconnect! next-reconnect]))

  (event-handler [:socket/connected new-state]))

(defmethod event-msg-handler :chsk/recv
  [{:as ev-msg :keys [event ?data]}]
  (debugf "Push event from server: %s" ?data)
  (event-handler ?data))

(defmethod event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid _ ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))

(def router_ (atom nil))

(defn start-ping-loop
  []
  (go
    (while true
      (async/<! (async/timeout 10000))
      (chsk-send!
        [:braid.client/ping]
        5000
        (fn [reply]
          (if-not (sente/cb-success? reply)
            (let [reconnect-time (+ 10000 (.valueOf (js/Date.)))]
              (dispatch [:core/websocket-disconnected!])
              (dispatch [:core/websocket-update-next-reconnect! reconnect-time]))
            (dispatch [:core/websocket-connected!])))))))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn disconnect! []
  (sente/chsk-disconnect! chsk))

(defn reconnect! []
  (sente/chsk-reconnect! chsk))

(defn connect! []
  (if (or (not chsk)
          (not (:csrf-token @chsk-state)))
    (xhr/edn-xhr
      {:uri "/csrf"
       :method :get
       :on-complete (fn [resp]
                      (if chsk
                        (do
                          (swap! chsk-state assoc :csrf-token (resp :token))
                          (reconnect!))
                        (do
                          (make-socket! (:token resp))
                          (start-router!)
                          (start-ping-loop))))})
    (reconnect!)))
