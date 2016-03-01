(ns chat.server.extensions
  (:require [environ.core :refer [env]])
  (:import org.apache.commons.codec.binary.Base64))

(defn str->b64
  [s]
  (-> s .getBytes Base64/encodeBase64 String.))

(defn b64->str
  [b64]
  (-> b64 .getBytes Base64/decodeBase64 String.))

(defn edn-response [clj-body & [status]]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)
   :status (or status 200)})

(def redirect-uri (str (env :site-url) "/extension/oauth"))
(def webhook-uri (str (env :site-url) "/extension/webhook"))

(defmulti handle-oauth-token (fn [ext state code] (get-in ext [:config :type])))
(defmulti handle-thread-change (fn [ext thread-id] (get-in ext [:config :type])))
(defmulti handle-webhook (fn [ext event-req] (get-in ext [:config :type])))
(defmulti extension-config (fn [ext data] (get-in ext [:config :type])))
