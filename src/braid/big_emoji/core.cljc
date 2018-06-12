(ns braid.big-emoji.core
  "If a message consists of a single emoji, displays it larger."
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[braid.emoji.client.views :refer [emoji-view]]])))

#?(:cljs
   (do
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
         (if (emoji? node)
           (update-in node [2 :class] #(str % " large"))
           node)))))

(defn init! []
  #?(:cljs
     (do
       (core/register-message-formatter!
         (fn [message]
           (if (single-emoji? message)
             (embiggen-emoji message)
             message))))))
