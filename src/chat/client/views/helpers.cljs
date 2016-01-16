(ns chat.client.views.helpers
  (:require [om.dom :as dom]
            [clojure.string :as string]
            [cljs-utils.core :refer [flip]]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [chat.client.emoji :as emoji]))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))


(def replacements
  {:urls
   {:pattern #"http(?:s)?://\S+(?:\w|\d|/)"
    :replace (fn [match]
               (dom/a #js {:href match :target "_blank"} match))}
   :users
   {:pattern #"@(\S*)"
    :replace (fn [match]
               (dom/span #js {:className "user-mention"} "@" match)) }
   :tags
   {:pattern #"#(\S*)"
    :replace (fn [match]
               (dom/span #js {:className "tag-mention"} "#" match))}
   :emoji-shortcodes
   {:pattern #"(:\S*:)"
    :replace (fn [match]
               (if (emoji/unicode match)
                 (emoji/shortcode->html match)
                 match))}
   :emoji-ascii
   {
    :replace (fn [match]
               (if-let [shortcode (emoji/ascii match)]
                 (emoji/shortcode->html shortcode)
                 match))}
   })

 ; should be able to do this with much less repetition

(defn url-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          pattern (get-in replacements [:urls :pattern])]
      (if-let [match (re-find pattern text)]
        ((get-in replacements [:urls :replace]) match)
        text))
    text-or-node))

(defn user-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          pattern (get-in replacements [:users :pattern])]
      (if-let [match (second (re-find pattern text))]
        ((get-in replacements [:users :replace]) match)
        text))
    text-or-node))

(defn tag-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          pattern (get-in replacements [:tags :pattern])]
      (if-let [match (second (re-find pattern text))]
        ((get-in replacements [:tags :replace]) match)
        text))
    text-or-node))

(defn emoji-shortcodes-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          pattern (get-in replacements [:emoji-shortcodes :pattern])]
      (if-let [match (second (re-find pattern text))]
        ((get-in replacements [:emoji-shortcodes :replace]) match)
        text))
    text-or-node))

(defn emoji-ascii-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node]
      (if (contains? emoji/ascii-set text)
        ((get-in replacements [:emoji-ascii :replace]) text)
        text))
    text-or-node))

(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  [text]
  (let [words (->> (string/split text #" ")
                   (map (fn [w]
                          (->> w
                               url-replace
                               user-replace
                               tag-replace
                               emoji-shortcodes-replace
                               emoji-ascii-replace)))
                   (interleave (repeat " "))
                   rest)]
    words))

(defn format-date
  "Turn a Date object into a nicely formatted string"
  [datetime]
  (let [datetime (t/to-default-time-zone datetime)
        now (t/to-default-time-zone (t/now))
        format (cond
                 (= (f/unparse (f/formatter "yyyydM") now)
                    (f/unparse (f/formatter "yyyydM") datetime))
                 "h:mm A"

                 (= (t/year now) (t/year datetime))
                 "h:mm A MMM d"

                 :else
                 "h:mm A MMM d yyyy")]
    (f/unparse (f/formatter format) datetime)))
