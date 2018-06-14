(ns braid.emoji.client.autocomplete
  (:require
    [clojure.string :as string]
    [braid.emoji.client.lookup :as lookup]
    [braid.emoji.client.views :refer [emoji-view]]))

(defn- shortcode->brackets
  "We allow user to search for emoji either using colons or brackets, ie. :shortcode: or (shortcode).
  Internally, the lookups are stored with colon shortcodes, ie. :shortcode:
  If the user is searching with brackets, we replace the colons with brackets when displaying results."
  [shortcode]
  (let [base (apply str (-> shortcode rest butlast))]
    (str "(" base ")")))

(defn- emoji-result-view
  [shortcode emoji-meta show-as-brackets?]
  [:div.emoji.match
   [emoji-view shortcode emoji-meta]
   [:div.info
    [:div.name (if show-as-brackets?
                 (shortcode->brackets shortcode)
                 shortcode)]
    [:div.extra]]])

(defn autocomplete-handler [text]
  (let [pattern #"\B[:(](\S{2,})$"]
    (when-let [query (second (re-find pattern text))]
      (let [show-as-brackets? (= "(" (first text))]
        (->> (lookup/shortcode)
             (filter (fn [[shortcode _]]
                       (string/includes? shortcode query)))
             (map (fn [[shortcode emoji-meta]]
                    {:key
                     (fn [] shortcode)
                     :action
                     (fn [])
                     :message-transform
                     (fn [text]
                       (string/replace text pattern (str shortcode " ")))
                     :html
                     (fn []
                       [emoji-result-view shortcode emoji-meta show-as-brackets?])})))))))
