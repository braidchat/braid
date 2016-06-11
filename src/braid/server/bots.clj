(ns braid.server.bots
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [cognitect.transit :as transit]
            [chat.server.crypto :as crypto])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

(defn ->transit
  [form]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :msgpack)]
    (transit/write writer form)
    (.toByteArray out)))

(defn send-notification
  [bot message]
  (let [body (->transit message)
        hmac (crypto/hmac-bytes (bot :token) body)]
    (timbre/debugf "sending bot notification")
    (http/put (bot :webhook-url)
              {:headers {"Content-Type" "application/transit+msgpack"
                         "X-Braid-Signature" hmac}
               :body (ByteArrayInputStream. body)})))
