(ns chat.server.digest
  (:require [clojure.java.io :as io])
  (:import [java.security MessageDigest]
           [org.apache.commons.codec.binary Base64]))

(defn- sha256-digest [bs]
  (doto (MessageDigest/getInstance "SHA-256") (.update bs)))

(defn sha256 [msg]
  (-> msg .getBytes sha256-digest .digest))

(defn from-file
  [f]
  (when-let [file (io/resource f)]
    (-> file
        slurp
        sha256
        Base64/encodeBase64
        String.)))
