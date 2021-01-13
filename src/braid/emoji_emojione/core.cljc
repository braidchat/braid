(ns braid.emoji-emojione.core
  "Provides emojione-style emoji"
  (:require
    [braid.emoji.api :as emoji]
    #?@(:cljs
         [[braid.emoji-emojione.impl :as impl]])))

(defn init! []
  #?(:cljs
     (do
       (emoji/register-emoji!
         {:shortcode-lookup impl/shortcode-lookup
          :ascii-lookup impl/ascii-lookup
          :styles impl/emojione-styles}))))
