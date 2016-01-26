(ns chat.server.util
  (:require [clojure.string :as string]))

(defn valid-nickname?
  "Checks if a nickname is well-formed. Does NOT check if already taken"
  [nick]
  (not (or (string/blank? nick)
           (< 30 (count nick))
           (re-find #"\s" nick)
           (re-find #"\p{Punct}" nick))))
