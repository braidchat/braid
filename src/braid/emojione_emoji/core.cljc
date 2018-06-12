(ns braid.emojione-emoji.core
  "Provides emojione-style emoji"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[braid.emoji.api :as emoji]
          [braid.emojione-emoji.impl :as impl]])))

(defn init! []
  #?(:cljs
     (do
       (emoji/register-emoji!
         {:shortcode-lookup impl/shortcode-lookup
          :ascii-lookup impl/ascii-lookup
          :styles impl/emojione-styles}))))
