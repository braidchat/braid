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
  (sync/chsk-send! [:rtc/send-protocol-signal {:sdp (.-sdp description)
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
  (let [candidate (.-candidate evt)]
    (when candidate
      (sync/chsk-send! [:braid.server/send-rtc-protocol-signal {:candidate (.-candidate candidate)
                                                                :sdpMid (.-sdpMid candidate)
                                                                :sdpMLineIndex (.-sdpMLineIndex candidate)}]))))

(defn handle-stream [evt]
  (let [stream (.-stream evt)
        stream-url (.. js/window -URL (createObjectURL stream))
        video-player (. js/document (getElementById "vid"))]
    (set! (.-src video-player) stream-url)
    (set! (.-onloadedmetadata video-player) (fn [_] (.play video-player)))))

; Connection

(defn create-connection [servers]
  (let [connection (js/webkitRTCPeerConnection. (clj->js {:iceServers servers}))]
    connection))

(defn create-local-connection [servers]
  (reset! local-peer-connnection (create-connection servers))
  (set! (.-onicecandidate @local-peer-connnection) handle-ice-candidate)
  (set! (.-onaddstream @local-peer-connnection) handle-stream))

(defn initialize-rtc-environment [servers]
  (when servers
    (create-local-connection servers)))
