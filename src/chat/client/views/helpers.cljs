(ns chat.client.views.helpers
  (:require [om.dom :as dom]
            [clojure.string :as string]))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",60%,60%)")))

(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  ; tried doing this with instaparse, but the js parser generator is way too slow, it seems
  [text]
  (let [url-re #"http(?:s)?://\S+(?:\w|\d|/)"
        text-links-seq (interleave
                         (map (partial assoc {} :text)
                              (string/split text url-re))
                         (map (partial assoc {} :link)
                              (re-seq url-re text)))]
    (if (empty? text-links-seq)
      (list text)
      (map
        (fn [elt]
          (if-let [link (elt :link)]
            (dom/a #js {:href link} link)
            (elt :text)))
        text-links-seq))))
