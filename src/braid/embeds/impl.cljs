(ns braid.embeds.impl
  (:require
    [braid.core.hooks :as hooks]))

(def url-re #"(http(?:s)?://\S+(?:\w|\d|/))")

(defn- extract-urls
  "Given some text, returns a sequence of URLs contained in the text"
  [text]
  (map first (re-seq url-re text)))

(defonce embed-engines (hooks/register! (atom [])))

(defn embed-view [message]
  (when-let [handler (->> @embed-engines
                          (sort-by :priority)
                          reverse
                          (some (fn [{:keys [handler]}]
                                  (handler {:urls (extract-urls (message :content))}))))]
    [:div.embed
     handler]))
