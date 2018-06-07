(ns braid.image-embeds.core
  "If a message contains a link to an image, displays the image as an embed"
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.embeds.core :as embeds]
          [braid.image-embeds.impl :as impl]])))

(defn init! []
  #?(:cljs
     (do
       (embeds/register-embed!
         {:handler impl/handler
          :styles impl/styles
          :priority 1}))))
