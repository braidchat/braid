(ns braid.emoji.client.lookup
  (:require
    [braid.core.hooks :as hooks]))

(defonce shortcode-fns
  (hooks/register! (atom []) [fn?]))

(defonce ascii-fns
  (hooks/register! (atom []) [fn?]))

(defn shortcode []
  (->> @shortcode-fns
       (map (fn [f] (f)))
       (apply merge {})))

(defn ascii []
  (->> @ascii-fns
       (map (fn [f] (f)))
       (apply merge {})))
