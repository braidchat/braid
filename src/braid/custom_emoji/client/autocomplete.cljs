(ns braid.custom-emoji.client.autocomplete
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe]]))

(defn lookup []
  (if-let [emoji @(subscribe [:custom-emoji/group-emojis])]
    (->> emoji
         (reduce (fn [memo {:keys [image shortcode]}]
                   (assoc memo shortcode {:class "custom-emoji"
                                          :src image})) {})
         (into {}))
    {}))
