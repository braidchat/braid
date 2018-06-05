(ns braid.custom-emoji.client.state
  (:require
    [schema.core :as s]
    [braid.custom-emoji.client.events]
    [braid.custom-emoji.client.remote-handlers]
    [braid.custom-emoji.client.subs]))

(defn- key-by [k coll]
  (reduce (fn [memo i]
            (assoc memo (k i) i)) {} coll))

(defn initial-data-handler
  [db data]
  (assoc db :custom-emoji/group-emoji
    (->> (data :custom-emoji/custom-emoji)
         (map (fn [[group-id emojis]]
                [group-id (key-by :shortcode emojis)]))
         (into {}))))

(def initial-state {:custom-emoji/group-emoji {}})

(def state-spec {:custom-emoji/group-emoji s/Any})
