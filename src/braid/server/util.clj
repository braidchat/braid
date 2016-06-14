(ns braid.server.util
  (:import org.apache.commons.validator.UrlValidator))

(defn valid-url?
  "Check if the string is a valid http(s) url.  Note that this will *not*
  accept local urls such as localhost or my-machine.local"
  ([s] (valid-url? s ["http" "https"]))
  ([s schemes]
   (and (string? s)
     (.isValid (UrlValidator. (into-array schemes)) s))))
