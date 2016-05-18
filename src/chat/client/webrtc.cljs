(ns chat.client.webrtc
  (:require [chat.client.sync :as sync]))

(defonce svga-dimensions
  (clj->js {:mandatory
            {:maxWidth 320
             :maxHeight 180}}))

(def local-peer-connnection (atom nil))

; Offers and Answers

(defn signal-session-description [description]
  (js/console.log "DESCRIPTION: " description))

(defn create-answer [connection]
  (.createAnswer @connection
                 signal-session-description
                 (fn [error]
                   (println "Error creating offer description:" (.-message error)))))

(defn create-offer [connection]
  (.createOffer @connection
                signal-session-description
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
  (println "Signal" signal))

; RTC Handlers

(defn handle-ice-candidate [evt]
  (let [candidate (.-candidate evt)]
    (js/console.log "CANDIDATE: " candidate)))

(defn handle-stream [evt]
  ;TODO: how to link up stream in ui
  (let [stream (.-stream evt)
        stream-url (.. js/window -URL (createObjectURL stream))]))

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
