(ns braid.lib.url
  (:import
   (goog Uri)))

(def url-re #"(http(?:s)?://\S+(?:\w|\d|/))")

(defn extract-urls
  "Given some text, returns a sequence of URLs contained in the text"
  [text]
  (map first (re-seq url-re text)))

(defn contains-urls? [text]
  (boolean (seq (extract-urls text))))

(defn url->parts [url]
  (let [url-info (.parse Uri url)]
    {:domain (.getDomain url-info)
     :path (.getPath url-info)
     :scheme (.getScheme url-info)
     :port (.getPort url-info)}))

(defn site-url
  []
  (let [{:keys [domain scheme port]} (url->parts (.-location js/window))]
    (str scheme "://" domain (when (or (and (= scheme "http")
                                       (not= port 80))
                                     (and (= scheme "https")
                                       (not= port 443)))
                             (str ":" port)))))
