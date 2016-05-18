(ns chat.client.webrtc
  (:require [chat.client.sync :as sync]))

(def local-peer-connnection (atom nil))

(defn handle-stream [evt]
  ;TODO: how to link up stream in ui
  (let [stream (.-stream evt)]))

(defn handle-ice-candidate [evt]
  (let [candidate (.-candidate evt)]
    ))

(defn create-connection [servers]
  (let [connection (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))]
    connection))

(defn create-local-connection [servers]
  (reset! local-peer-connnection (create-connection servers))
  (set! (.-onicecandidate @local-peer-connnection) handle-ice-candidate)
  (set! (.-onaddstream @local-peer-connnection) handle-stream)
  (js/console.log "Local Connection" @local-peer-connnection))

(defn initialize-rtc-environment [servers]
  (when servers
    (create-local-connection servers)))
