(ns chat.server.extensions
  (:require [environ.core :refer [env]]))

(def redirect-uri (str (env :site-url) "/extension/oauth"))
(def webhook-uri (str (env :site-url) "/extension/webhook"))

(defmulti handle-thread-change (fn [ext thread-id] (get-in ext [:config :type])))
(defmulti handle-webhook (fn [ext event-req] (get-in ext [:config :type])))
