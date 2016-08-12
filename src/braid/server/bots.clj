(ns braid.server.bots
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [braid.server.crypto :as crypto]
            [braid.server.util :refer [->transit]])
  (:import java.io.ByteArrayInputStream))

(defn send-notification
  [bot message]
  (let [body (->transit message)
        hmac (crypto/hmac-bytes (bot :token) body)]
    (timbre/debugf "sending bot notification")
    (println
      @(http/put (bot :webhook-url)
                 {:headers {"Content-Type" "application/transit+msgpack"
                            "X-Braid-Signature" hmac}
                  :body (ByteArrayInputStream. body)}))))
