(ns braid.embeds-website.link-extract
  (:require
    [clojure.core.memoize :as memo]
    [clojure.string :as string]
    [hickory.core :as h]
    [hickory.select :as hs]
    [org.httpkit.client :as http])
  (:import
    (java.net URL)
    (java.net URLDecoder)))

(defn absolute-link
  [url-base link]
  (cond
    (string/starts-with? link "http") link
    (string/starts-with? link "//") (str "https:" link)
    (string/starts-with? link "/") (str url-base link)
    :else (str url-base "/" link)))

(defn image-size
  [img-url]
  (some-> @(http/head img-url) :headers :content-length Long.))

(defn find-img
  [url-base host content]
  (let [url-re (re-pattern
                 (str "^http(?:s)?://(?:[^/])*" host ".*$"))]
    (some-> (hs/select
              (hs/descendant
                (hs/and (hs/or (hs/tag "div") (hs/tag "section"))
                        (hs/or (hs/id "main") (hs/id "body")
                               (hs/id "container")))
                (hs/and (hs/tag "img")
                        (hs/or
                          (hs/attr "src" #(string/starts-with? % "/"))
                          (hs/attr "src" (partial re-matches url-re)))))
              content)
            (->> (into
                   []
                   (comp (take 3)
                         (map #(get-in % [:attrs :src]))
                         (map (partial absolute-link url-base))))
                 (sort-by image-size #(compare %2 %1))
                 first))))

(defn image-is-big?
  [img-url]
  (some-> @(http/head img-url)
          :headers :content-length Long.
          ; TODO: adjust lenth based on image type
          (> 50000)))

(defn extract'
  [url]
  (let [response @(http/head url)
        headers (-> response :headers)
        content (when (some-> headers :content-type
                              (string/starts-with? "text/html"))
                  (-> @(http/get url) :body h/parse h/as-hickory))
        u (URL. url)
        [proto host port] ((juxt (memfn getProtocol)
                                 (memfn getHost)
                                 (memfn getPort))
                           u)
        url-base (str proto "://" host
                      (when (not= -1 port) (str ":" port)))
        page-image (some->> content (find-img url-base host) )
        page-type (if (nil? (:content-type headers))
                    "text"
                    (condp re-matches host
                      #"(www\.)?youtube.com" "video"
                      #"youtu.be" "video"
                      #"(www\.)?vimeo.com" "video"

                      (condp #(string/starts-with? %2 %1) (:content-type headers)
                        "text/html"  "html"
                        "text/plain" "text"
                        "image/"     "photo"
                        nil)))]
    {:url url
     :original_url url
     :title (or (and content
                     (some-> (hs/select (hs/tag "title") content) first :content
                             string/join))
                "")
     :favicon_url (or (and content
                           (some-> (hs/select
                                     (hs/and (hs/tag "link")
                                             (hs/attr "rel" #{"icon" "shortcut icon"}))
                                     content)
                                   first
                                   (get-in [:attrs :href])
                                   (->> (absolute-link url-base))))
                      "")
     :type (if (and page-image (= page-type "html")
                    (image-is-big? page-image))
             "photo"
             page-type)

     :provider_name (-> (re-matches #"^(?:www\.)?([^.]+)\.(?:.*)$" host)
                        second
                        (or host)
                        string/capitalize)
     :images (cond
               (and (re-matches #"(www\.)?youtube.com" host)
                    (not (string/blank? (.getQuery u))))
               (let [qs (into {}
                              (map #(vec (string/split % #"=" 2)))
                              (-> (.getQuery u)
                                  (string/split #"&")))
                     v-id (URLDecoder/decode (qs "v"))]
                 [{:url (str "https://i.ytimg.com/vi/" v-id "/hqdefault.jpg")
                   :colors [{:color [0 0 0]}]}])

               (= "photo" page-type)
               [{:url url :colors [{:color [0 0 0]}]}]

               :else
               (when page-image
                 [{:url page-image
                   :colors [{:color [0 0 0]}]}]))}))

(def extract (memo/ttl extract' :ttl/threshold (* 5 60 1000)))
