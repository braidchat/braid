(ns braid.emoji.client.views)

(defn emoji-view [shortcode emoji-meta]
  [:img {:class ["emoji" (emoji-meta :class)]
         :alt shortcode
         :title shortcode
         :src (emoji-meta :src)}])
