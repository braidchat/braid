(ns braid.youtube-embeds.core
  "Detects youtube links and includes embedded video player"
  (:require
    [braid.embeds.api :as embeds]))

(defn- youtube-embed-view
  [video-id]
  [:iframe
   {:src (str "https://www.youtube-nocookie.com/embed/" video-id "?rel=0")
    :frame-border 0
    :allow "encrypted-media; autoplay"
    :allow-full-screen true}])

(defn init! []
  #?(:cljs
     (do
       (embeds/register-embed!
         {:handler
          (fn [{:keys [urls]}]
            (when-let [video-id (->> urls
                                     (some (fn [url]
                                             (or
                                               (second (re-matches #"^https?://youtu.be/(.*)" url))
                                               (second (re-matches #"^https?://www\.youtube\.com/watch\?v=(.*)" url))))))]
              [youtube-embed-view video-id]))

          :styles
          [:>video
           {:width "100%"}]

          :priority 1}))))
