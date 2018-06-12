(ns braid.video-embeds.core
  "If a message contains a link to a video, displays the video as an embed"
  (:require
    [braid.embeds.api :as embeds]))

(defn- video-embed-view
  [url]
  [:video
   {:src url
    :preload "metadata"
    :controls true}])

(defn init! []
  #?(:cljs
     (do
       (embeds/register-embed!
         {:handler
          (fn [{:keys [urls]}]
            (when-let [url (->> urls
                                (some (fn [url]
                                        (re-matches #".*(mp4|mkv|mov)$" url)))
                                first)]
              [video-embed-view url]))

          :styles
          [:>video
           {:width "100%"}]

          :priority 1}))))
