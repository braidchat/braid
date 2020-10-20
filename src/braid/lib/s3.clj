(ns braid.lib.s3
  (:require
    [clojure.data.json :as json]
    [clojure.string :as string]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [org.httpkit.client :as http]
    [braid.base.conf :refer [config]]
    [braid.lib.crypto :as crypto :refer [hmac-sha256 str->bytes]])
  (:import
    (org.joda.time DateTime DateTimeZone Period)
    (org.joda.time.format ISODateTimeFormat)))

(defn generate-policy
  []
  (when-let [secret (config :aws-secret-key)]
    ;; why is this using joda via java interop instead of clj-time?
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
                     crypto/base64-encode)]
      {:bucket (config :aws-domain)
       :auth {:policy policy
              :key (config :aws-access-key)
              :signature (crypto/b64-sha1-encode policy secret)}})))

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

(defn upload-url-path
  [url]
  (some->
    (re-pattern (str "^https://s3.amazonaws.com/"
                     (config :aws-domain)
                     "(/.*)$"))
    (re-matches url)
    second))

(defn delete-upload
  [upload-path]
  @(http/request (make-request {:method :delete
                                :body ""
                                :path upload-path})))
