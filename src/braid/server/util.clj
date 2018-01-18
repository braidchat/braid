(ns braid.server.util
  (:require
    [clojure.string :as string]
    [cognitect.transit :as transit])
  (:import
    (java.net URLEncoder)
    (java.io ByteArrayOutputStream ByteArrayInputStream)
    (com.cognitect.transit Writer)
    (com.cognitect.transit.impl MsgpackEmitter WriteCache  WriteHandlerMap)
    (org.msgpack MessagePack)
    (org.apache.commons.validator UrlValidator)))

(defn valid-url?
  "Check if the string is a valid http(s) url.  Note that this will *not*
  accept local urls such as localhost or my-machine.local"
  ([s] (valid-url? s ["http" "https"]))
  ([s schemes]
   (and (string? s)
     (or
       (string/starts-with? s "http://localhost:")
       (.isValid (UrlValidator. (into-array schemes)) s)))))

(defn map->query-str
  [m]
  (->> m
       (map (fn [[k v]] (str (name k) "=" (URLEncoder/encode (str v)))))
       (string/join "&")))

;; Transit stuff

(defn fake-cache
  []
  (proxy [WriteCache] []
    (isCacheable [_ _] false)
    (cacheWrite [s _] s)))

(defn noncaching-writer
  [^java.io.OutputStream out]
  (let [handlers (WriteHandlerMap. ^java.util.Map transit/default-write-handlers)
        packer (.createPacker (MessagePack.) out)
        emitter (MsgpackEmitter. packer handlers)
        write-cache (fake-cache)
        wrtr (reify Writer
               (write [_ o]
                 (try
                   (.emit emitter o false (.init write-cache))
                   (.flush out)
                   (catch Throwable e
                     (throw (RuntimeException. e))))))]
    (transit/->Writer wrtr)))

(defn ->transit ^bytes
  [form]
  (let [out (ByteArrayOutputStream. 4096)
        ;; writer (transit/writer out :msgpack)
        writer (noncaching-writer out)]
    (transit/write writer form)
    (.toByteArray out)))

(defn transit->form
  [^bytes ts]
  (let [in (ByteArrayInputStream. ts)]
    (transit/read (transit/reader in :msgpack))))
