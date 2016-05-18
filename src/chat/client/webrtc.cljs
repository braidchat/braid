(ns chat.client.webrtc)

(def local-peer-connnection (atom nil))

(defn create-connection [servers]
  (let [connection (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))]
    connection))

(defn create-local-connection [servers]
  (reset! local-peer-connnection (create-connection servers))
  (js/console.log "Local Connection" @local-peer-connnection))

(defn initialize-rtc-environment [servers]
  (when servers
    (create-local-connection servers)))
