(ns braid.lib.aws
  (:require
   [clojure.string :as string])
  (:import
   (java.net URLEncoder)
   (java.security MessageDigest)
   (java.time Instant ZoneId)
   (java.time.format DateTimeFormatter)
   (javax.crypto Mac)
   (javax.crypto.spec SecretKeySpec)))

(defn str->bytes
  ^bytes [s]
  (.getBytes s "UTF-8"))

(defn bytes->hex
  [^bytes bs]
  (->> bs (map (partial format "%02x")) (apply str)))

(defn hmac-sha256
  ^bytes [^bytes key-bytes ^bytes to-sign-bytes]
  (let [mac (Mac/getInstance "HmacSHA256")
        secret-key (SecretKeySpec. key-bytes (.getAlgorithm mac))]
    (-> (doto mac (.init secret-key))
        (.doFinal to-sign-bytes))))

(defn sha256
  ^bytes [^bytes input-bytes]
  (-> (doto (MessageDigest/getInstance "SHA-256")
        (.update input-bytes))
      (.digest)))

(def hex-hash (comp bytes->hex sha256 str->bytes))

(defn signing-key
  "Generate a signing key for an AWS request"
  [{:keys [aws-api-secret day service aws-region]}]
  (-> (str "AWS4" aws-api-secret)
      str->bytes
      (hmac-sha256 (str->bytes day))
      (hmac-sha256 (str->bytes aws-region))
      (hmac-sha256 (str->bytes service))
      (hmac-sha256 (str->bytes "aws4_request"))))

(def basic-date-format (DateTimeFormatter/ofPattern "yyyyMMdd"))
(def basic-date-time-format (DateTimeFormatter/ofPattern "yyyyMMdd'T'HHmmssX"))
(def date-time-format (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssXX"))

(defn utc-now
  []
  (.atZone (Instant/now) (ZoneId/of "UTC")))

(defn url-encode-path
  [path]
  (string/replace
   path
   #"[^-._/a-zA-Z0-9]"
   (fn [[m]]
     (format "%%%02X" (int m)))))

(defn canonical-request
  [{:keys [method path headers body query-string]}]
  (->>
   [method
    (url-encode-path path)
    (if query-string
      (->> (string/split query-string #"&")
           (map #(string/split % #"="))
           (map (fn [[k v]] (str (URLEncoder/encode k "UTF-8")
                                 "="
                                 (URLEncoder/encode v "UTF-8"))))
           sort
           (string/join "&"))
      "")
    (->> headers
         (map (fn [[h v]] (str (string/lower-case h) ":" v "\n")))
         sort
         string/join)
    (->> headers keys (map string/lower-case) sort (string/join ";"))
    (if body (hex-hash body) "UNSIGNED-PAYLOAD")]
   (string/join "\n")
   hex-hash))

(defn auth-header
  [{:keys [now service aws-api-secret aws-api-key aws-region]
    {:keys [headers] :as request} :request}]
  (let [day (.format now basic-date-format)
        date (.format now basic-date-time-format)
        signed-headers (->> headers keys
                            (map string/lower-case)
                            sort
                            (string/join ";"))
        str-to-sign (->>
                     ["AWS4-HMAC-SHA256"
                      date
                      (string/join "/" [day aws-region service "aws4_request"])
                      (canonical-request request)]
                     (string/join "\n"))
        signed (-> (hmac-sha256
                    (signing-key {:day day :service service
                                  :aws-api-secret aws-api-secret
                                  :aws-region aws-region})
                    (str->bytes str-to-sign))
                   bytes->hex)]
    (str
     "AWS4-HMAC-SHA256 "
     "Credential=" (string/join "/" [aws-api-key day aws-region service "aws4_request"]) ","
     "SignedHeaders=" signed-headers ","
     "Signature=" signed)))
