(ns braid.emoji.client.lookup
  (:require
    [braid.core.hooks :as hooks]))

(defonce shortcode-fns (hooks/register! (atom [])))

(defonce ascii-fns (hooks/register! (atom [])))

(defn shortcode []
  (->> @shortcode-fns
       (map (fn [f] (f)))
       (apply merge {})))

(defn ascii []
  (->> @ascii-fns
       (map (fn [f] (f)))
       (apply merge {})))
