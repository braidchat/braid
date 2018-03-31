(ns braid.emoji.client.core
  (:require
   [braid.emoji.client.events]
   [braid.emoji.client.remote-handlers]
   [braid.emoji.client.subs]
   [schema.core :as s]))

(defn initial-data-handler
  [db data]
  (prn "custom emoji" (data :emoji/custom-emoji))
  (assoc
    db ::group-emoji
    (into {}
          (map (fn [[g-id emojis]]
                 [g-id (->> (group-by :shortcode emojis)
                           (reduce-kv #(assoc %1 %2 (first %3)) {}))]))
          (data :emoji/custom-emoji))))

(def state
  [{::group-emoji {}}
   {::group-emoji s/Any}])
