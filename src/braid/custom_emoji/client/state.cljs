(ns braid.custom-emoji.client.state
  (:require
    [schema.core :as s]
    [braid.custom-emoji.client.events]
    [braid.custom-emoji.client.remote-handlers]
    [braid.custom-emoji.client.subs]))

(defn initial-data-handler
  [db data]
  (assoc
    db :custom-emoji/group-emoji
    (into {}
          (comp
            (map (fn [[g-id emojis]]
                   [g-id
                    (->> emojis
                        (group-by :shortcode)
                        (reduce-kv #(assoc %1 %2 (first %3)) {}))])))
          (data :custom-emoji/custom-emoji))))

(def initial-state {:custom-emoji/group-emoji {}})

(def state-spec {:custom-emoji/group-emoji s/Any})
