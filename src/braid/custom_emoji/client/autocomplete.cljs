(ns braid.custom-emoji.client.autocomplete
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe]]
    [braid.lib.upload :as upload]))

(defn lookup []
  (if-let [emoji @(subscribe [:custom-emoji/group-emojis])]
    (->> emoji
         (reduce (fn [memo {:keys [image shortcode]}]
                   (assoc memo shortcode {:class "custom-emoji"
                                          :src (upload/->path image)})) {})
         (into {}))
    {}))
