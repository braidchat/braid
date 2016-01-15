(ns chat.client.views.helpers
  (:require [om.dom :as dom]
            [clojure.string :as string]
            [cljs-utils.core :refer [flip]]
            [cljs-time.format :as f]
            [cljs-time.core :as t]))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))


(def replacements
  {:urls {:pattern #"http(?:s)?://\S+(?:\w|\d|/)"
          :replace (fn [match]
                     (dom/a #js {:href match :target "_blank"} match))}
   :users {:pattern #"@(\S*)"
           :replace (fn [match]
                      (dom/span #js {:className "user-mention"} "@" match)) }
   :tags {:pattern #"#(\S*)"
          :replace (fn [match]
                     (dom/span #js {:className "tag-mention"} "#" match))}})

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

(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  [text]
  (let [words (->> (string/split text #" ")
                   (map (fn [w]
                          (->> w
                               url-replace
                               user-replace
                               tag-replace)))
                   (interleave (repeat " "))
                   rest)]
    words))

(defn format-date
  "Turn a Date object into a nicely formatted string"
  [date]
  (f/unparse (f/formatter "h:mm A") (t/to-default-time-zone date)))
