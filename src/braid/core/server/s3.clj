(ns braid.core.server.s3
  (:require
    [clojure.data.json :as json]
    [clojure.string :as string]
    [braid.core.server.conf :refer [config]])
  (:import
   (javax.crypto Mac)
   (javax.crypto.spec SecretKeySpec)
   (org.apache.commons.codec.binary Base64)
   (org.joda.time DateTime DateTimeZone Period)
   (org.joda.time.format ISODateTimeFormat)))

(defn base64-encode
  [input]
  (-> input
      (.getBytes "UTF-8")
      Base64/encodeBase64
      String.))

(defn b64-sha1-encode
  "Get the base64-encode HMAC-SHA1 of `to-sign` with `key`"
  [to-sign key]
  (let [mac (Mac/getInstance "HmacSHA1")
        secret-key (SecretKeySpec. (.getBytes key "UTF-8") (.getAlgorithm mac))]
    (-> (doto mac (.init secret-key))
        (.doFinal (.getBytes to-sign "UTF-8"))
        Base64/encodeBase64
        String.)))

(defn generate-policy
  []
  (when-let [secret (config :s3-upload-secret)]
    (let [policy (-> {:expiration (.. (DateTime. (DateTimeZone/UTC))
                                      (plus (Period/minutes 5))
                                      (toString (ISODateTimeFormat/dateTimeNoMillis)))
                      :conditions
                      [
                       {:bucket (config :aws-domain)}
                       ["starts-with" "$key" ""]
                       {:acl "public-read"}
                       ["starts-with" "$Content-Type" ""]
                       ["content-length-range" 0 524288000]]}
                     json/write-str
                     base64-encode)]
      {:bucket (config :aws-domain)
       :auth {:policy policy
              :key (config :s3-upload-key)
              :signature (b64-sha1-encode policy secret)}})))
