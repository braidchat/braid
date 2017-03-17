(ns braid.server.api.link-extract
  (:require
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

(defn extract
  [url]
  (let [response @(http/get url)
        content (-> response :body h/parse h/as-hickory)
        headers (-> response :headers)
        u (URL. url)
        [proto host port] ((juxt (memfn getProtocol)
                                 (memfn getHost)
                                 (memfn getPort))
                           u)
        url-base (str proto "://" host
                      (when-not (not= -1 port) (str ":" port)))]
    {:url url
     :original_url url
     :title (some-> (hs/select (hs/tag "title") content) first :content
                    string/join)
     :favicon_url (some-> (hs/select
                            (hs/and (hs/tag "link")
                                    (hs/attr "rel" #{"icon" "shortcut icon"}))
                            content)
                          first
                          (get-in [:attrs :href])
                          (->> (str url-base)))
     :media {:type
             (condp re-matches host
               #"(www\.)?youtube.com" "video"
               #"youtu.be" "video"
               #"(www\.)?vimeo.com" "video"

               (condp #(string/starts-with? %2 %1) (:content-type headers)
                 "text/html"  "html"
                 "text/plain" "text"
                 "image/"     "photo"
                 nil))}

     :provider_name (-> (re-matches #"^(?:www\.)?([^.]+)\.(?:.*)$" host)
                        second
                        (or host)
                        string/capitalize)
     :images (if (re-matches #"(www\.)?youtube.com" host)
               (let [qs (into {}
                              (map #(vec (string/split % #"=" 2)))
                              (-> (.getQuery u)
                                  (string/split #"&")))
                     v-id (URLDecoder/decode (qs "v"))]
                 [{:url (str "https://ytimg.com/vi/" v-id "/default.jpg")
                  :colors [{:color [0 0 0]}]}])
               (when-let [img (find-img host content)]
                 [{:url img
                  :colors [{:color [0 0 0]}]}]))}))
