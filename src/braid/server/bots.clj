(ns braid.server.bots
  (:require [org.httpkit.client :as http]
            [cognitect.transit :as transit]
            [chat.server.crypto :as crypto])
  (:import [java.io ByteArrayOutputStream]))

(defn ->transit
  [form]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :msgpack)]
    (transit/write writer form)
    (.toString out)))

(defn send-notification
  [bot message]
  ; TODO: need some sort of signing/verification
  (let [body (->transit message)]
    (http/put (bot :webhook-url)
              {:headers {"Content-Type" "application/transit+msgpack"
                         "X-Braid-Signature" ""}
               :body body})))
