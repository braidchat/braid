(ns braid.server.crypto
  (:require [clojure.string :as string])
  (:import javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           java.security.SecureRandom
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

(defn hmac-bytes
  [hmac-key data-bytes]
  (let [key-bytes (.getBytes hmac-key "UTF-8")
        algo "HmacSHA256"]
    (->>
      (doto (Mac/getInstance algo)
        (.init (SecretKeySpec. key-bytes algo)))
      (#(.doFinal % data-bytes))
      (map (partial format "%02x"))
      (apply str))))

(defn hmac
  [hmac-key data]
  (hmac-bytes hmac-key (.getBytes data "UTF-8")))

(defn constant-comp
  "Compare two strings in constant time"
  [a b]
  (loop [a a b b match (= (count a) (count b))]
    (if (and (empty? a) (empty? b))
      match
      (recur
        (rest a)
        (rest b)
        (and match (= (first a) (first b)))))))

(defn hmac-verify
  [{:keys [secret mac data]}]
  (constant-comp mac (hmac secret data)))
