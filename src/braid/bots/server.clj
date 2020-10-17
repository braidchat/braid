(ns braid.bots.server
  "Sending notification of messages and events to bots"
  (:require
   [braid.core.server.crypto :as crypto]
   [braid.lib.transit :refer [->transit]]
   [org.httpkit.client :as http]
   [taoensso.timbre :as timbre])
  (:import
   (java.io ByteArrayInputStream)))

(defn send-message-notification
  [bot message]
  (let [body (->transit message)
        hmac (crypto/hmac-bytes (bot :token) body)]
    (timbre/debugf "sending bot notification")
    (try
      ; TODO: should this be a POST too?
      (->>
        @(http/put (bot :webhook-url)
                   {:headers {"Content-Type" "application/transit+msgpack"
                              "X-Braid-Signature" hmac}
                    :body (ByteArrayInputStream. body)})
        (timbre/debugf "Bot response: %s"))
      (catch Exception ex
        (timbre/warnf "Error sending bot notification: %s" ex)))))

(defn send-event-notification
  [bot info]
  (when-let [url (:event-webhook-url bot)]
    (timbre/debugf "Sending event notification %s to %s" info bot)
    (let [body (->transit info)
          hmac (crypto/hmac-bytes (bot :token) body)]
      (timbre/debugf "sending bot event notification")
      (try
        (->>
          @(http/post url
                      {:headers {"Content-Type" "application/transit+msgpack"
                                 "X-Braid-Signature" hmac}
                       :body (ByteArrayInputStream. body)})
          (timbre/debugf "Bot response: %s"))
        (catch Exception ex
          (timbre/warnf "Error sending bot event notification: %s" ex))))))
