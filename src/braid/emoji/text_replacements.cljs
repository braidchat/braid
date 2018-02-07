(ns braid.emoji.text-replacements
  (:require
   [braid.emoji.helpers :as emoji]
   [clojure.string :as string]))

(defn emoji-shortcodes-replace
  [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node
          replace-fn (fn [match]
                       (if (emoji/unicode match)
                         (emoji/shortcode->html match)
                         match))
          re #"(:\S*:)"
          ;; using js split b/c we don't want the match in the last
          ;; component
          [pre _ post] (seq (.split text re replace-fn))]
      (if-let [match (second (re-find re text))]
        (if (or (string/blank? pre) (re-matches #".*\s$" pre))
          [:span.dummy pre (replace-fn match) post]
          text)
        text))
    text-or-node))

(defn emoji-ascii-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node]
      (if (contains? emoji/ascii-set text)
        (if-let [shortcode (emoji/ascii text)]
          (emoji/shortcode->html shortcode)
          text)
        text))
    text-or-node))

(defn shortcode-emoji?
  [message-part]
  (and (= 4 (count message-part))
       (= :span.dummy (first message-part))
       (= :img (get-in message-part [2 0]))
       (string/starts-with? (get-in message-part [2 1 :class]) "emojione")))

(defn ascii-emoji?
  [message-part]
  (and (= 2 (count message-part))
       (= :img (first message-part))
       (string/starts-with? (get-in message-part [1 :class]) "emojione")))

(defn emoji?
  [message-part]
  (and (vector? message-part)
       (or (shortcode-emoji? message-part)
           (ascii-emoji? message-part))))

(defn format-emojis
  "Formats messages containing only emojis (ignoring whitespace). When
  a message meets the criteria, all emojis will have the class 'large'
  appended to the end of their class attribute. All other messages are
  returned unmodified"
  [message]
  (let [text (filterv #(or (not (string? %))
                           (not (some? (re-find #"\s+" %))))
                      message)]
    (if (every? emoji? text)
      (map (fn [part]
             (cond (shortcode-emoji? part)
                   (update-in part [2 1 :class] #(str % " large"))

                   (ascii-emoji? part)
                   (update-in part [1 :class] #(str % " large"))

                   :else part))
           message)
      message)))
