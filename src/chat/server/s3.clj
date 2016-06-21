(ns chat.server.s3
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [braid.server.conf :refer [config]])
  (:import javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           sun.misc.BASE64Encoder
           (org.joda.time DateTime DateTimeZone Period)
           (org.joda.time.format DateTimeFormat ISODateTimeFormat)))

(defn base64-encode
  [input]
  (-> (BASE64Encoder.)
      (.encode (.getBytes input "UTF-8"))
      (string/replace #"\n" "")))

(defn b64-sha1-encode
  "Get the base64-encode HMAC-SHA1 of `to-sign` with `key`"
  [to-sign key]
  (let [mac (Mac/getInstance "HmacSHA1")
        secret-key (SecretKeySpec. (.getBytes key "UTF-8") (.getAlgorithm mac))]
    (-> (doto mac (.init secret-key))
        (.doFinal (.getBytes to-sign "UTF-8"))
        (->> (.encode (BASE64Encoder.))))))

(defn generate-upload-policy
  []
  (when-let [secret (config :s3-upload-secret)]
    (let [policy (->
                   {:expiration (.. (DateTime. (DateTimeZone/UTC))
                                    (plus (Period/minutes 5))
                                    (toString (ISODateTimeFormat/dateTimeNoMillis)))
                    :conditions [{:bucket (config :aws-domain)}
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

(defn generate-list-policy
  [group-id]
  (when-let [secret (config :s3-upload-secret)]
    (let [now (.. (DateTime. (DateTimeZone/UTC))
                  (toString (DateTimeFormat/forPattern "E, dd MMM yyyy HH:mm:ss Z")))
          policy (string/join
                   \newline
                   ["GET"
                    ""
                    ""
                    ""
                    (str "x-amz-date:" now)
                    (str "/" (config :aws-domain) "/")])]
      {:bucket (config :aws-domain)
       :headers {"x-amz-date" now
                 "Authorization" (str "AWS " (config :s3-upload-key)
                                      ":" (b64-sha1-encode policy secret))}})))
