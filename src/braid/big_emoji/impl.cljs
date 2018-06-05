(ns braid.big-emoji.impl
  (:require
    [clojure.string :as string]
    [braid.emoji.client.views :refer [emoji-view]] ;FIXME non public API
    ))

(defn- emoji? [node]
  (and
    (vector? node)
    (= emoji-view (first node))))

(defn- single-emoji? [message]
  (let [emoji-count (->> message
                         (filter emoji?)
                         count)
        other-count (- (count message) emoji-count)]
    (and
      (= emoji-count 1)
      (= other-count 0))))

(defn- embiggen-emoji [message]
  (for [node message]
    (do (if (emoji? node)
          (update-in node [2 :class] #(str % " large"))
          node))))

(defn format-emojis
  "If a message contains only a single emoji, adds the large class to the emoji. Otherwise, returns the message unmodified."
  [message]
  (if (single-emoji? message)
    (embiggen-emoji message)
    message))
