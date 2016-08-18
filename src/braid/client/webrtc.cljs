(ns braid.client.webrtc
  (:require [braid.client.sync :as sync]
            [braid.client.dispatcher :refer [dispatch!]]))

(defonce svga-dimensions
  (clj->js {:mandatory
            {:maxWidth 320
             :maxHeight 180}}))

(def local-peer-connnection (atom nil))
(def remote-peer-connection (atom nil))

; Offer/Answer

;TODO: handle somewhere else - can't get connection

(defn signal-sdp-description [description]
  (.setLocalDescription @local-peer-connnection description)
  (sync/chsk-send!
    [:braid.server/send-rtc-protocol-signal
      {:sdp (aget description "sdp")
       :type (aget description "type")}]))

(defn create-answer [connection]
  (.createAnswer connection signal-sdp-description
    (fn [error]
      (println "Error creating offer description: " (aget error "message")))))

(defn create-offer [connection]
  (.createOffer connection signal-sdp-description
    (fn [error]
      (println "Error creating offer description: " (aget error "message")))))

(defn handle-protocol-signal [signal]
  (if (signal :candidate)
    (.addIceCandidate @local-peer-connnection (js/RTCIceCandidate. (clj->js signal)))
    (do
      (.setRemoteDescription @local-peer-connnection (js/RTCSessionDescription. (clj->js signal)))
      (when (= "offer" (signal :type))
        (create-answer local-peer-connnection)))))

; Media

(defn open-local-stream [call]
  (let [local-connection (call :local-connection)
        stream-success (fn [stream]
                         (.addStream local-connection stream)
                         (dispatch! :create-sdp-offer local-connection))
        stream-failure (fn [error]
                         (println "Error opening stream: " (aget error "message")))]
    (. js/navigator
       (webkitGetUserMedia
         (clj->js {:audio true :video svga-dimensions}) stream-success stream-failure))))

; Setup

(defn handle-ice-candidate [evt]
  (let [candidate (aget evt "candidate")]
    (sync/chsk-send!
      [:braid.server/send-rtc-protocol-signal
        {:candidate (aget candidate "candidate")
         :sdpMid (aget candidate "sdpMid")
         :sdpMLineIndex (aget candidate "sdpMLineIndex")}])))

(defn handle-stream [evt]
  (let [stream (aget evt "stream")
        stream-url (aset js/window "URL" (.createObjectURL stream))
        video-player (. js/document (getElementById "video"))]
    (aset video-player "src" stream-url)
    (aset video-player "onloadedmetadata" (fn [_] (.play video-player)))))

(defn create-local-connection [servers]
  (let [connection (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))]
    (aset connection "onicecandidate" handle-ice-candidate)
    (aset connection "onaddstream" handle-stream)
    (reset! local-peer-connnection connection)
    (js/console.log @local-peer-connnection)))

(defn get-ice-servers [handler]
  (sync/chsk-send! [:braid.server/get-ice-servers] 2500
    (fn [servers]
      (if (= servers :chsk/timeout)
        (get-ice-servers handler) ; TODO TRY AGAIN
        (handler servers)))))
