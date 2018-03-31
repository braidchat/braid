(ns braid.emoji.client.core
  (:require
   [braid.emoji.client.events]
   [braid.emoji.client.remote-handlers]
   [braid.emoji.client.subs]
   [schema.core :as s]))

(defn initial-data-handler
  [db data]
  (assoc
    db ::group-emoji
    (into {}
          (comp
            (map (fn [[g-id emojis]]
                   [g-id
                    (->> emojis
                        (group-by :shortcode)
                        (reduce-kv #(assoc %1 %2 (first %3)) {}))])))
          (data :emoji/custom-emoji))))

(def state
  [{::group-emoji {}}
   {::group-emoji s/Any}])
