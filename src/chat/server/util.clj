(ns chat.server.util
  (:require [clojure.string :as string]))

(defn valid-nickname?
  "Checks if a nickname is well-formed. Does NOT check if already taken"
  [nick]
  (not (or (string/blank? nick)
           (< 30 (count nick))
           (re-find #"\s" nick)
           (re-find #"\p{Punct}" nick))))

(defn valid-tag-name?
  "Checks if a tag name is valid, but not if already taken"
  [tag]
  (not (or (string/blank? tag)
           (< 30 (count tag))
           (re-find #"\s" tag)
           (re-find #"\p{Punct}" tag))))
