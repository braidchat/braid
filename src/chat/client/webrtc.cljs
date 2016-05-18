(ns chat.client.webrtc
  (:require [chat.client.sync :as sync]))

(defonce svga-dimensions
  (clj->js {:mandatory
            {:maxWidth 320
             :maxHeight 180}}))

(def local-peer-connnection (atom nil))

; Offers and Answers

(defn signal-sdp-answer [description]
  (js/console.log "LOCAL ANSWER:" description))

(defn signal-sdp-offer [description]
  (js/console.log "LOCAL DESC: " description)
  (.setLocalDescription @local-peer-connnection description)
  (sync/chsk-send! [:rtc/send-protocol-info {:sdp (.-sdp description)
                                             :type (.-type description)}]))

(defn create-answer [connection]
  (.createAnswer @connection
                 signal-sdp-answer
                 (fn [error]
                   (println "Error creating offer description:" (.-message error)))))

(defn create-offer [connection]
  (.createOffer @connection
                signal-sdp-offer
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
    (let [received-desc (js/RTCSessionDescription. (clj->js {:sdp (signal :sdp)
                                                             :type (signal :type)}))]
      (js/console.log "REMOTE DESC:" received-desc)
      (.setRemoteDescription @local-peer-connnection received-desc)
      (create-answer local-peer-connnection))
    (let [received-cand (js/RTCIceCandidate. (clj->js {:candidate (signal :candidate)
                                                       :sdpMid (signal :sdpMid)
                                                       :sdpMLineIndex (signal :sdpMLineIndex)}))]
      (js/console.log "REMOTE CAND:" received-cand)
      (.addIceCandidate @local-peer-connnection received-cand))))

; RTC Handlers

(defn handle-ice-candidate [evt]
  (let [candidate (.-candidate evt)]
    #_(js/console.log "CANDIDATE: " candidate)
    (println "LOCAL CAND:" candidate)
    (when candidate
      (sync/chsk-send! [:rtc/send-protocol-info {:candidate (.-candidate candidate)
                                                 :sdpMid (.-sdpMid candidate)
                                                 :sdpMLineIndex (.-sdpMLineIndex candidate)}]))))

(defn handle-stream [evt]
  ;TODO: how to link up stream in ui
  (let [stream (.-stream evt)
        stream-url (.. js/window -URL (createObjectURL stream))]
    (println "onaddstream" stream)))

; Connection

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
