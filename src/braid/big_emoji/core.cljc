(ns braid.big-emoji.core
  "If a message consists of a single emoji, displays it larger."
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.big-emoji.impl :refer [format-emojis]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-message-formatter! format-emojis))))
