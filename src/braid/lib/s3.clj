(ns braid.lib.s3
  (:require
   [clojure.data.json :as json]
   [clojure.string :as string]
   [org.httpkit.client :as http]
   [braid.base.conf :refer [config]]
   [braid.lib.aws :as aws])
  (:import
   (java.net URLEncoder)
   (java.time.temporal ChronoUnit)
   (org.apache.commons.codec.binary Base64 Hex)))

(defn s3-host
  [{bucket :aws-bucket region :aws-region}]
  (str bucket ".s3." region ".amazonaws.com"))

(defn generate-s3-upload-policy
  [{:aws/keys [credentials-provider] region :aws-region bucket :aws-bucket :as config} {:keys [starts-with]}]
  (when credentials-provider
    (let [[api-key api-secret ?security-token] (credentials-provider)
          utc-now (aws/utc-now)
          day (.format utc-now aws/basic-date-format)
          date (.format utc-now aws/basic-date-time-format)
          credential (->> [api-key day region "s3" "aws4_request"]
                          (string/join "/"))
          policy (-> {:expiration (-> (.plus utc-now 5 ChronoUnit/MINUTES)
                                      (.format aws/date-time-format))
                      :conditions
                      [{:bucket bucket}
                       ["starts-with" "$key" starts-with]
                       {:acl "private"}
                       ["starts-with" "$Content-Type" ""]
                       ["content-length-range" 0 (* 500 1024 1024)]

                       {"x-amz-algorithm" "AWS4-HMAC-SHA256"}
                       {"x-amz-credential" credential}
                       {"x-amz-date" date}]}
                     (cond-> ?security-token
                       (update :conditions conj
                               {"x-amz-security-token" ?security-token}))
                     json/write-str
                     aws/str->bytes
                     Base64/encodeBase64String)]
      {:bucket bucket
       :region region
       :auth {:policy policy
              :key api-key
              :signature (->>
                           (aws/str->bytes policy)
                           (aws/hmac-sha256
                             (aws/signing-key
                               {:aws-api-secret api-secret
                                :aws-region region
                                :day day
                                :service "s3"}))
                           Hex/encodeHexString)
              :credential credential
              :date date}})))

(defn- get-signature
  [{:aws/keys [credentials-provider] region :aws-region bucket :aws-bucket :as config} utc-now path query-str]
  (let [[_ api-secret ?security-token] (credentials-provider)]
    (->> ["AWS4-HMAC-SHA256"
          (.format utc-now aws/basic-date-time-format)
          (string/join "/" [(.format utc-now aws/basic-date-format) region "s3"
                            "aws4_request"])
          (aws/canonical-request
           {:method "GET"
            :path (str "/" path)
            :query-string query-str
            :headers (cond-> {"host" (str bucket ".s3." region ".amazonaws.com")}
                       ?security-token
                       (assoc "x-amz-security-token" ?security-token))
            :body nil})]
         (string/join "\n")
         aws/str->bytes
         (aws/hmac-sha256
          (aws/signing-key {:aws-api-secret api-secret
                            :aws-region region
                            :day (.format utc-now aws/basic-date-format)
                            :service "s3"}))
         aws/bytes->hex)))

(defn readable-s3-url
  [{:aws/keys [credentials-provider] region :aws-region :as config} expires-seconds path]
  (let [[api-key _ ?security-token] (credentials-provider)
        utc-now (aws/utc-now)
        query-str (str "X-Amz-Algorithm=AWS4-HMAC-SHA256"
                       "&X-Amz-Credential=" api-key "/" (.format utc-now aws/basic-date-format) "/" region "/s3/aws4_request"
                       "&X-Amz-Date=" (.format utc-now aws/basic-date-time-format)
                       (str "&X-Amz-Expires=" expires-seconds)
                       "&X-Amz-SignedHeaders=host"
                       (when ?security-token
                         (str "&X-Amz-Security-Token=" (URLEncoder/encode ?security-token "UTF-8"))) )]
    (str "https://" (s3-host config)
         "/" path
         "?" query-str
         "&X-Amz-Signature=" (get-signature config utc-now path query-str)
         )))

(defn- make-request
  [{:aws/keys [credentials-provider] region :aws-region bucket :aws-bucket :as config} {:keys [body method path] :as request}]
  (let [[api-key api-secret ?session-token] (credentials-provider)
        utc-now (aws/utc-now)
        req (cond->
                (update request :headers
                        assoc
                        "x-amz-date" (.format utc-now aws/basic-date-time-format)
                        "x-amz-content-sha256" (aws/hex-hash body)
                        "Host" (str (s3-host config) ":443"))
              ?session-token (assoc-in [:headers "x-amz-security-token"]
                                       ?session-token))]
    (-> req
        (dissoc :method :path :query-string)
        (assoc-in [:headers "Authorization"]
                  (aws/auth-header {:now utc-now
                                    :service "s3"
                                    :request req
                                    :aws-api-secret api-secret
                                    :aws-api-key api-key
                                    :aws-region region}))
        (cond->
            ?session-token
          (assoc-in [:headers "x-amz-security-token"] ?session-token)))))



(defn delete-file!
  [config path]
  ;; always returns 204, even if file does not exist
  @(http/delete (str "https://" (s3-host config) path)
                (make-request config {:method "DELETE"
                                      :path path
                                      :body ""})))

(defn upload-url-path
  [url]
  (when url
    (or ;; old style, with bucket after domain
     (second (re-matches #"^https://s3\.amazonaws\.com/[^/]+(/.*)$" url))
     ;; new style, with bucket in domain
     (second (re-matches #"^https://.+\.amazonaws\.com(/.*)$" url)))))
