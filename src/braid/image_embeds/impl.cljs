(ns braid.image-embeds.impl)

(def styles
  [:>.image
   [:>img
    {:width "100%"}]])

(defn image-embed-view
  [url]
  [:a.image
   {:href url
    :target "_blank"
    :rel "noopener noreferrer"}
   [:img {:src url}]])

(defn handler
  [{:keys [urls]}]
  (when-let [url (->> urls
                      (some (fn [url]
                              (re-matches #".*(png|jpg|jpeg|gif)$" url)))
                      first)]
    [image-embed-view url]))
