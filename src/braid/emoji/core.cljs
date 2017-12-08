(ns braid.emoji.core
  (:require
    [clojure.string :as string]
    [braid.core.api :as api]
    [braid.emoji.helpers :as helpers]
    [braid.client.ui.views.new-message :refer [register-autocomplete-engine!]]))

(defn simple-matches?
  [m s]
  (not= -1 (.indexOf m s)))

(defn emoji-view
  [emoji]
  [:div.emoji.match
   (helpers/shortcode->html (string/replace emoji #"[\(\)]" ":"))
   [:div.name emoji]
   [:div.extra "..."]])

(defn autocomplete-handler [text]
  (let [pattern #"\B[:(](\S{2,})$"]
    (when-let [query (second (re-find pattern text))]
      (->> helpers/unicode
           (filter (fn [[k v]]
                     (simple-matches? k query)))
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

(defn init! []
  (register-autocomplete-engine! autocomplete-handler))
