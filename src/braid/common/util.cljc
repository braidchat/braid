(ns braid.common.util
  (:require
    [clojure.string :as string]))

; TODO: we should probably define this by a whitelist.  Unicode ranges?
(def nickname-re
  #"(?:[^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]{1,30})")

;Duplicating the above regex instead of just doing (re-pattern (str ...
;nickname-re ...)) because in clojurescript, going from regex->string->regex
;it gets wrapped in slashes.  Closed as wontfix :(
(def sigiled-nickname-re
  "as nickname-re, but with the @sigil before it and capturing the name"
  #"(?:\s|^)@([^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]{1,30})")

(def tag-name-re
  #"(?:[^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~]{1,50})")

(def sigiled-tag-name-re
  #"(?:\s|^)#([^ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~]{1,50})")


(defn valid-nickname?
  "Checks if a nickname is well-formed. Does NOT check if already taken"
  [nick]
  (and (not (string/blank? nick)) (re-matches nickname-re nick)))

(defn valid-tag-name?
  "Checks if a tag name is valid, but not if already taken"
  [tag]
  (and (not (string/blank? tag)) (re-matches tag-name-re tag)))

(defn reversed
  "Given a function f, return a function that passes arguments to f in the reversed order"
  [f]
  (fn [& args]
    (apply f (reverse args))))

(def bot-name-re
  #"(?:\w|\d){1,30}")

; cljs already has cljs.core/uuid?
#?(:clj
   (defn uuid?
     [u]
     (instance? java.util.UUID u)))

(defn valid-email? [email]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? email) (re-matches pattern email))))

(defn remove-non-whitelist-characters [text]
  (string/replace text (re-pattern #"[^a-z^A-Z^0-9^\-]*") ""))

(defn slugify
  "Converts text to a slug-safe representation"
  [text]
  (when text
    (-> text
        string/trim
        string/lower-case
        remove-non-whitelist-characters)))
