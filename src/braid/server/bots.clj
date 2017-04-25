(ns braid.server.bots
  (:require
    [org.httpkit.client :as http]
    [taoensso.timbre :as timbre]
    [braid.server.crypto :as crypto]
    [braid.server.util :refer [->transit]])
  (:import
    java.io.ByteArrayInputStream))

(defn send-message-notification
  [bot message]
  (let [body (->transit message)
        hmac (crypto/hmac-bytes (bot :token) body)]
    (timbre/debugf "sending bot notification")
    (println
      ; TODO: should this be a POST too?
      @(http/put (bot :webhook-url)
                 {:headers {"Content-Type" "application/transit+msgpack"
                            "X-Braid-Signature" hmac}
                  :body (ByteArrayInputStream. body)}))))

(defn send-event-notification
  [bot info]
  (when-let [url (:event-webhook-url bot)]
    (timbre/debugf "Sending event notification %s to %s" info bot)
    (let [body (->transit info)
          hmac (crypto/hmac-bytes (bot :token) body)]
      (timbre/debugf "sending bot event notification")
      (println
        @(http/post url
                    {:headers {"Content-Type" "application/transit+msgpack"
                               "X-Braid-Signature" hmac}
                     :body (ByteArrayInputStream. body)})))))
