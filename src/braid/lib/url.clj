(ns braid.lib.url
  (:require
    [clojure.string :as string])
  (:import
    (java.net URLEncoder)
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

