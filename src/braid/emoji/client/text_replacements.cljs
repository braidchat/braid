(ns braid.emoji.client.text-replacements
  (:require
   [clojure.string :as string]
   [braid.emoji.client.lookup :as lookup]
   [braid.emoji.client.views :refer [emoji-view]]))

(defn emoji-shortcodes-replace
  [node]
  (if (string? node)
    (if-let [match (re-matches #":\S*:" node)]
      (if-let [emoji-meta ((lookup/shortcode) match)]
        [emoji-view match emoji-meta]
        node)
      node)
    node))

(defn emoji-ascii-replace [node]
  (if (string? node)
    (if-let [emoji-meta ((lookup/ascii) node)]
      [emoji-view node emoji-meta]
      node)
    node))
