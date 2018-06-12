(ns braid.image-embeds.core
  "If a message contains a link to an image, displays the image as an embed"
  (:require
    [braid.embeds.api :as embeds]))

(defn image-embed-view
  [url]
  [:a.image
   {:href url
    :target "_blank"
    :rel "noopener noreferrer"}
   [:img {:src url}]])

(defn init! []
  #?(:cljs
     (do
       (embeds/register-embed!
         {:handler
          (fn [{:keys [urls]}]
            (when-let [url (->> urls
                                (some (fn [url]
                                        (re-matches #".*(png|jpg|jpeg|gif)$" url)))
                                first)]
              [image-embed-view url]))

          :styles
          [:>.image
           [:>img
            {:width "100%"}]]

          :priority 1}))))
