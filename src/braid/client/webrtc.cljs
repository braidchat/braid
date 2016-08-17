(ns braid.client.webrtc
  (:require [braid.client.sync :as sync]))

(defonce svga-dimensions
  (clj->js {:mandatory
            {:maxWidth 320
             :maxHeight 180}}))

(def local-peer-connnection (atom nil))

; Offers and Answers

(defn signal-sdp-description [description]
  (.setLocalDescription @local-peer-connnection description)
  (sync/chsk-send! [:braid.server/send-rtc-protocol-signal {:sdp (.-sdp description)
                                                            :type (.-type description)}]))

(defn create-answer [connection]
  (.createAnswer
    @connection
    signal-sdp-description
    (fn [error]
      (println "Error creating offer description:" (.-message error)))))

(defn create-offer [connection]
  (.createOffer
    @connection
    signal-sdp-description
    (fn [error]
      (println "Error creating offer description:" (.-message error)))))

; Media

(defn open-local-stream [stream-type]
  (let [constraints (atom nil)
        stream-success (fn [stream]
                         (.addStream @local-peer-connnection stream)
                         (create-offer local-peer-connnection))
        stream-failure (fn [error]
                         (println "Error opening stream:" (.-message error)))]
    (if (= stream-type "audio")
      (reset! constraints (clj->js {:audio true :video false}))
      (reset! constraints (clj->js {:audio true :video svga-dimensions})))
    (. js/navigator (webkitGetUserMedia @constraints stream-success stream-failure))))

; Protocol Exchange

(defn handle-protocol-signal [signal]
  (if (signal :sdp)
    (let [remote-description (js/RTCSessionDescription. (clj->js {:sdp (signal :sdp)
                                                                  :type (signal :type)}))]
      (.setRemoteDescription @local-peer-connnection remote-description)
      (when (= "offer" (signal :type))
        (create-answer local-peer-connnection)))
    (let [remote-candidate (js/RTCIceCandidate. (clj->js {:candidate (signal :candidate)
                                                          :sdpMid (signal :sdpMid)
                                                          :sdpMLineIndex (signal :sdpMLineIndex)}))]
      (.addIceCandidate @local-peer-connnection remote-candidate))))

; RTC Handlers

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
        video-player (. js/document (getElementById "vid"))]
    (aset video-player "src" stream-url)
    (aset video-player "onloadedmetadata" (fn [_] (.play video-player)))))

; Connection

(defn create-local-connection [servers]
  (let [connection (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))]
    (aset connection "onicecandidate" handle-ice-candidate)
    (aset connection "onaddstream" handle-stream)
    connection))
