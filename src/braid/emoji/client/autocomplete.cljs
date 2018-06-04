(ns braid.emoji.client.autocomplete
  (:require
    [clojure.string :as string]
    [braid.emoji.client.helpers :as helpers]))

(defn emoji-view
  [emoji]
  [:div.emoji.match
   (helpers/shortcode->html (string/replace emoji #"[\(\)]" ":"))
   [:div.name emoji]
   [:div.extra "..."]])

(defn autocomplete-handler [text]
  (let [pattern #"\B[:(](\S{2,})$"]
    (when-let [query (second (re-find pattern text))]
      (->> (helpers/matching query)
           (map (fn [[k v]]
                  {:key
                   (fn [] k)
                   :action
                   (fn [])
                   :message-transform
                   (fn [text]
                     (string/replace text pattern (str k " ")))
                   :html
                   (fn []
                     [emoji-view (let [show-brackets? (= "(" (first text))
                                       emoji-name (apply str (-> k rest butlast))]
                                   (if show-brackets?
                                     (str "(" emoji-name ")") k))
                      {:react-key k}])}))))))
