(ns braid.video-embeds.impl)

(def styles
  [:>video
   {:width "100%"}])

(defn video-embed-view
  [url]
  [:video
   {:src url
    :preload "metadata"
    :controls true}])

(defn handler
  [{:keys [urls]}]
  (when-let [url (->> urls
                      (some (fn [url]
                              (re-matches #".*(mp4|mkv|mov)$" url)))
                      first)]
    [video-embed-view url]))
