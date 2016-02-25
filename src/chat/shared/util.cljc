(ns chat.shared.util
  (:require [clojure.string :as string]))

; TODO: we should probably define this by a whitelist.  Unicode ranges?
(def nickname-re
  #"(?:[^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]{1,30})")

;Duplicating the above regex instead of just doing (re-pattern (str ...
;nickname-re ...)) because in clojurescript, going from regex->string->regex
;it gets wrapped in slashes.  Closed as wontfix :(
(def sigiled-nickname-re
  "as nickname-re, but with the @sigil before it and capturing the name"
  #"(?:\s|^)@([^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]{1,30})(?=\s|$)")

(def tag-name-re
  #"(?:[^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~]{1,50})")

(def sigiled-tag-name-re
  #"(?:\s|^)#([^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~]{1,50})(?=\s|$)")


(defn valid-nickname?
  "Checks if a nickname is well-formed. Does NOT check if already taken"
  [nick]
  (re-matches nickname-re nick))

(defn valid-tag-name?
  "Checks if a tag name is valid, but not if already taken"
  [tag]
  (re-matches tag-name-re tag))

(defn reversed
  "Given a function f, return a function that passes arguments to f in the reversed order"
  [f]
  (fn [& args]
    (apply f (reverse args))))

(defn if?
  "Given a predicate function, then function and else function, return a fn
  that returns (then x) if (pred x) returns true and (else x) otherwise"
  [pred t e]
  (fn [x] (if (pred x) (t x) (e x))))
