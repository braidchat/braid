(ns braid.client.webrtc
  (:require [braid.client.sync :as sync]
            [braid.client.dispatcher :refer [dispatch!]]))

(defonce svga-dimensions
  (clj->js {:mandatory {:maxWidth 320 :maxHeight 180}}))

(def sending-peer-connection (atom nil))

(def receiving-peer-connection (atom nil))

; REMOTE STUFF

; Protocols

(defn signal-sdp-description [connection description]
  (.setLocalDescription @connection description)
  (sync/chsk-send!
    [:braid.server/send-protocol-signal
      {:sdp (aget description "sdp")
       :type (aget description "type")}]))

(defn create-answer [connection]
  (.createAnswer
    @connection
    (fn [description]
      (signal-sdp-description connection description))
    (fn [error]
      (println "Error creating offer description: " (aget error "message")))))

(defn create-offer [connection]
  (.createOffer
    @connection
    (fn [description]
      (signal-sdp-description connection description))
    (fn [error]
      (println "Error creating offer description: " (aget error "message")))))

(defn receive-protocol-signal [signal]
  (if (signal :candidate)
    (.addIceCandidate @sending-peer-connection (js/RTCIceCandidate. (clj->js signal)))
    (do
      (.setRemoteDescription @sending-peer-connection (js/RTCSessionDescription. (clj->js signal)))
      (when (= "offer" (signal :type))
        (create-answer sending-peer-connection)))))

; Media

(defn set-stream [peer-connection]
  (letfn [(stream-success [stream]
            (.addStream @peer-connection stream)
            (create-offer peer-connection))
          (stream-failure [error]
            (println "Error opening stream: " (aget error "message")))]
    (. js/navigator
       (webkitGetUserMedia
         (clj->js {:audio true :video svga-dimensions}) stream-success stream-failure))))

(defn set-callee-stream []
  (set-stream receiving-peer-connection))

(defn set-caller-stream []
  (set-stream sending-peer-connection))

; Setup

(defn handle-ice-candidate [evt]
  (let [candidate (aget evt "candidate")]
    (when candidate
      (sync/chsk-send!
        [:braid.server/send-protocol-signal
          {:candidate (aget candidate "candidate")
           :sdpMid (aget candidate "sdpMid")
           :sdpMLineIndex (aget candidate "sdpMLineIndex")}]))))

(defn handle-stream [evt]
  (let [stream (aget evt "stream")
        stream-url (.. js/window -URL (createObjectURL stream))
        video-player (. js/document (getElementById "vid"))]
    (aset video-player "src" stream-url)
    (aset video-player "onloadedmetadata" (fn [_] (.play video-player)))))

(defn set-connection-atom [conn conn-atom on-ice on-stream]
  (aset conn "onicecandidate" on-ice)
  (aset conn "onaddstream" on-stream)
  (reset! conn-atom conn)
  (js/console.log @conn-atom))

(defn create-connections [servers]
  (set-connection-atom
    (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))
    sending-peer-connection
    handle-ice-candidate
    handle-stream)
  #_(set-connection-atom
    (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))
    receiving-peer-connection
    handle-ice-candidate
    handle-stream))

(defn get-ice-servers [handler]
  (sync/chsk-send! [:braid.server/get-ice-servers] 2500
    (fn [servers]
      (if (= servers :chsk/timeout)
        (get-ice-servers handler)
        (handler servers)))))
