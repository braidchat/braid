(ns braid.emojione-emoji.core
  "Provides emojione-style emoji"
  (:require
    [braid.emoji.api :as emoji]
    #?@(:cljs
         [[braid.emojione-emoji.impl :as impl]])))

(defn init! []
  #?(:cljs
     (do
       (emoji/register-emoji!
         {:shortcode-lookup impl/shortcode-lookup
          :ascii-lookup impl/ascii-lookup
          :styles impl/emojione-styles}))))
