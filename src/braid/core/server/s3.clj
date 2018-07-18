(ns braid.core.server.s3
  (:require
   [braid.core.server.conf :refer [config]]
   [braid.core.server.crypto :as crypto :refer [hmac-sha256 str->bytes]]
   [clj-time.core :as t]
   [clj-time.format :as f]
   [clojure.data.json :as json]
   [clojure.string :as string])
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

(defn signing-key
  "Generate a signing key for an AWS request"
  [{:keys [day service]}]
  (let [aws-api-secret (config :aws-secret-key)
        aws-region (or (config :aws-region) "us-east-1")]
    (-> (str "AWS4" aws-api-secret)
        str->bytes
        (hmac-sha256 (str->bytes day))
        (hmac-sha256 (str->bytes aws-region))
        (hmac-sha256 (str->bytes service))
        (hmac-sha256 (str->bytes "aws4_request")))))

(defn canonical-request
  [{:keys [method path headers body]}]
  (->>
    [(string/upper-case (name method))
     path
     ""
     (->> headers
          (map (fn [[h v]] (str (string/lower-case h) ":" v "\n")))
          sort
          string/join)
     (->> headers keys (map string/lower-case) sort (string/join ";"))
     (crypto/hex-hash body)]
    (string/join "\n")
    crypto/hex-hash))

(defn auth-header
  [{:keys [date service] {:keys [headers method path body] :as request} :request}]
  (let [aws-region (or (config :aws-region) "us-east-1")
        aws-api-key (config :aws-access-key)
        day (f/unparse (f/formatter :basic-date) date)
        date (f/unparse (f/formatter :basic-date-time-no-ms) date)
        signed-headers (->> headers keys (map string/lower-case) sort (string/join ";"))
        str-to-sign (->>
                      ["AWS4-HMAC-SHA256"
                       date
                       (string/join "/" [day aws-region service "aws4_request"])
                       (canonical-request request)]
                      (string/join "\n"))
        signed (-> (hmac-sha256
                     (signing-key {:day day :service service})
                     (str->bytes str-to-sign))
                   crypto/bytes->hex)]
    (str
      "AWS4-HMAC-SHA256 "
      "Credential=" (string/join "/" [aws-api-key day aws-region service "aws4_request"]) ","
      "SignedHeaders=" signed-headers ","
      "Signature=" signed)))

(defn make-request
  [{:keys [body method path]}]
  (let [now (t/now)
        date (f/unparse (f/formatter :basic-date-time-no-ms) now)
        req {:headers {"x-amz-date" date
                       "x-amz-content-sha256" (crypto/hex-hash body)
                       "Host" "s3.amazonaws.com"}
             :body body
             :method method
             :path (str "/" (config :aws-domain) path)}
        auth (auth-header {:date now :service "s3" :request req})]
    (-> req (assoc-in [:headers "Authorization"] auth)
        (dissoc :path)
        (assoc :url (str "https://s3.amazonaws.com/" (config :aws-domain) path)))))
