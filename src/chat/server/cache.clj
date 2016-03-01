(ns chat.server.cache
  (:require [clojure.string :as string]
            [environ.core :refer [env]]
            [taoensso.carmine :as car])
  (:import java.security.SecureRandom
           [org.apache.commons.codec.binary Base64]))

(defn random-nonce
  "url-safe random nonce"
  [size]
  (let [rand-bytes (let [seed (byte-array size)]
                     (.nextBytes (SecureRandom. ) seed)
                     seed)]
    (-> rand-bytes
        Base64/encodeBase64
        String.
        (string/replace "+" "-")
        (string/replace "/" "_")
        (string/replace "=" ""))))

; same as conf in handler, but w/e
(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

(def prod? (= (env :environment) "prod"))
(def dev-cache
  "Cache used in place of redis when running in dev/demo mode"
  (atom {}))

(defn cache-set! [k v]
  (if prod?
    (car/wcar redis-conn (car/set k v))
    (swap! dev-cache assoc k v)))

(defn cache-get [k]
  (if prod?
    (car/wcar redis-conn (car/get k))
    (@dev-cache k)))

(defn cache-del! [k]
  (if prod?
    (car/wcar redis-conn (car/del k))
    (swap! dev-cache dissoc k)))

(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

