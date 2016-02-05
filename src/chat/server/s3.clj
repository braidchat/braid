(ns chat.server.s3
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [environ.core :refer [env]])
  (:import javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           sun.misc.BASE64Encoder
           (org.joda.time DateTime Period)
           org.joda.time.format.ISODateTimeFormat))

(defn base64-encode
  [input]
  (-> (BASE64Encoder.)
      (.encode (.getBytes input "UTF-8"))
      (string/replace #"\n" "")))

(defn b64-sha1-encode
  "Get the base64-encode HMAC-SHA1 of `to-sign` with `key`"
  [to-sign key]
  (let [mac (Mac/getInstance "HmacSHA1")
        secret-key (SecretKeySpec. (.getBytes key "UTF-8") (.getAlgorithm mac))
        b64-encode (fn [m] (.encode (BASE64Encoder.) m))]
    (-> (doto mac (.init secret-key))
        (.doFinal (.getBytes to-sign "UTF-8"))
        b64-encode)))

(defn generate-policy
  []
  (when-let [secret (env :s3-upload-secret)]
    (let [expiry (.. (DateTime.) (plus (Period/minutes 5)) (toString (ISODateTimeFormat/basicDateTimeNoMillis)))
          policy (-> {:expiration expiry
                      :conditions
                      [
                       {:bucket "cdn.thenext36.ca"}
                       ["starts-with" "$key" ""]
                       {:acl "public-read"}
                       ["starts-with" "$Content-Type" ""]
                       ["content-length-range" 0 524288000]]}
                     json/write-str
                     base64-encode)]
      {:policy policy
       :signature (b64-sha1-encode policy secret)})))
