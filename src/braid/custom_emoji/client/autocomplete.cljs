(ns braid.custom-emoji.client.autocomplete
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe]]))

(defn simple-matches?
  [m s]
  (not= -1 (.indexOf m s)))

(defn shortcode->html [shortcode]
  (when-let [custom @(subscribe [:custom-emoji/custom-emoji shortcode])]
    [:img {:class "emojione"
           :alt shortcode
           :title shortcode
           :src (custom :image)}]))

(defn emoji-view
  [emoji]
  [:div.emoji.match
   (shortcode->html (string/replace emoji #"[\(\)]" ":"))
   [:div.name emoji]
   [:div.extra "..."]])

(defn unicode [shortcode]
  (get @(subscribe [:custom-emoji/custom-emoji shortcode]) :image))

(defn matching
  [query]
  (mapcat
    (partial filter (fn [[code _]] (simple-matches? code query)))
    [(map (fn [{:keys [shortcode image]}] [shortcode image])
          @(subscribe [:custom-emoji/group-emojis]))]))

(defn autocomplete-handler [text]
  (let [pattern #"\B[:(](\S{2,})$"]
    (when-let [query (second (re-find pattern text))]
      (->> (matching query)
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

(defn emoji-shortcodes-replace
  [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          replace-fn (fn [match]
                       (if (unicode match)
                         (shortcode->html match)
                         match))
          re #"(:\S*:)"
          ;; using js split b/c we don't want the match in the last
          ;; component
          [pre _ post] (seq (.split text re replace-fn))]
      (if-let [match (second (re-find re text))]
        (cond
          (not (unicode match))
          text-or-node

          (or (string/blank? pre) (re-matches #".*\s$" pre))
          [:span.dummy pre (replace-fn match) post]

          :else
          text)
        text))
    text-or-node))
