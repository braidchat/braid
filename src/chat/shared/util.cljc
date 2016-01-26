(ns chat.shared.util
  (:require [clojure.string :as string]))

(def punctuation-re
  "Regex to match disallowed punctuation.  Not just using \\p{Punct} in clj, as
  we want to allow underscores, dashes, and slashes"
  #"[\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~]")

(defn valid-nickname?
  "Checks if a nickname is well-formed. Does NOT check if already taken"
  [nick]
  (not (or (string/blank? nick)
           (< 30 (count nick))
           (re-find #"\s" nick)
           (re-find punctuation-re nick))))

(defn valid-tag-name?
  "Checks if a tag name is valid, but not if already taken"
  [tag]
  (not (or (string/blank? tag)
           (< 50 (count tag))
           (re-find #"\s" tag)
           (re-find punctuation-re tag))))
