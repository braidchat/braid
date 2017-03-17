(ns braid.server.api.link-extract
  (:require
    [clojure.core.memoize :as memo]
    [clojure.string :as string]
    [hickory.core :as h]
    [hickory.select :as hs]
    [org.httpkit.client :as http])
  (:import
    (java.net URL)
    (java.net URLDecoder)))

(defn find-img
  [host content]
  (let [url-re (re-pattern
                 (str "^http(?:s)?://(?:[^/])*" host ".*$"))]
    (some-> content
            (hs/select
              (hs/and (hs/tag "img")
                      (hs/attr "src" (partial re-matches url-re))))
            first
            (get-in [:attrs :src]))))

(defn extract'
  [url]
  (let [response @(http/get url)
        headers (-> response :headers)
        content (when (string/starts-with? (:content-type headers) "text/html")
                  (-> response :body h/parse h/as-hickory))
        u (URL. url)
        [proto host port] ((juxt (memfn getProtocol)
                                 (memfn getHost)
                                 (memfn getPort))
                           u)
        url-base (str proto "://" host
                      (when (not= -1 port) (str ":" port)))
        page-type (condp re-matches host
                    #"(www\.)?youtube.com" "video"
                    #"youtu.be" "video"
                    #"(www\.)?vimeo.com" "video"

                    (condp #(string/starts-with? %2 %1) (:content-type headers)
                      "text/html"  "html"
                      "text/plain" "text"
                      "image/"     "photo"
                      nil))]
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
                               (->> (str url-base))))
                      "")
     :media {:type page-type}
     :type page-type

     :provider_name (-> (re-matches #"^(?:www\.)?([^.]+)\.(?:.*)$" host)
                        second
                        (or host)
                        string/capitalize)
     :images (cond
               (re-matches #"(www\.)?youtube.com" host)
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
               (when-let [img (and content (find-img host content))]
                 [{:url img
                   :colors [{:color [0 0 0]}]}]))}))

(def extract (memo/ttl extract' :ttl/threshold (* 5 60 1000)))
