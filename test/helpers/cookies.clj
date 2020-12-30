(ns helpers.cookies
  (:require
   [ring.util.codec :as codec]
   [clojure.string :as string])
  (:import
   (java.time Instant)
   (java.time.format DateTimeFormatter)))

(let [parser DateTimeFormatter/RFC_1123_DATE_TIME]
  (defn- parse-rfc1123 [s]
    "Parse RFC822/RFC1123 date per http://tools.ietf.org/html/rfc2616#section-3.3.1"
    (.. parser (parseDateTime s) getMillis)))

(let [matchers [#(when-let [match (re-find #"Max-Age=(\d+)" %)]
                   [:max-age (.. Instant now (plusMillis (long (* 1000 (Integer/parseInt (match 1))))) toEpochMilli)])
                #(when-let [match (re-find #"Expires=(.+)" %)]
                   [:expires (parse-rfc1123 (match 1))])
                #(when-let [match (re-find #"Domain=(.+)" %)]
                   [:domain (match 1)])
                #(when-let [match (re-find #"Path=(.*)" %)]
                   [:path (match 1)])
                #(when (= "Secure" %) [:secure true])
                #(when (= "HttpOnly" %) [:http-only true])]]

  (defn- parse-cookie-attrs [attr-string]
    "Parse optional cookie attributes per RFC 6265"
    (let [attrs (string/split attr-string #";")
          matches (map (fn [a] (some (fn [m] (m a)) matchers)) attrs)]
      ;; max-age dominates expires
      (reduce (fn [m [k v]] (if (= :max-age k) (merge {:expires v} m) (merge m {k v}))) {} matches))))

(defn parse-cookie [c]
  "Parse cookies per http://tools.ietf.org/html/rfc6265"
  (let [x (re-find #"([^;]+)(?:;(.*))*" c)
        [name value] (first (codec/form-decode (x 1)))
        av-map (parse-cookie-attrs (x 2))]
    {name (merge av-map {:value value})}))

(defn write-cookie [[name {value :value}]]
  (codec/form-encode {name value}))

(defn write-cookies [cookies]
  (string/join ";" (map write-cookie cookies)))
