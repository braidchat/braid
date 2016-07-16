(ns braid.server.digest
  (:require [clojure.java.io :as io])
  (:import [java.security MessageDigest]
           [org.apache.commons.codec.binary Base64]))

(defn sha256-digest
  "Byte array to sha256"
  [^bytes bs]
  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))

(defn from-file
  [f]
  (when-let [file (io/resource f)]
    (-> file
        slurp
        .getBytes
        sha256-digest
        Base64/encodeBase64
        String.)))
