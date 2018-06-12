(ns braid.embeds.core
  "Allows for extending Braid with embed handlers, which can display inline content after a message, based on the content of the message"
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.embeds.impl :as impl]
          [braid.embeds.styles :as styles]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-styles! styles/embed)
       (core/register-footer-view! impl/embed-view))))
