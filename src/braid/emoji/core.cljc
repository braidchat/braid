(ns braid.emoji.core
  "Allows other modules to define emoji"
  (:require
    [braid.core.api :as core]
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.emoji.client.autocomplete :refer [autocomplete-handler]]
          [braid.emoji.client.text-replacements :refer [emoji-ascii-replace
                                                         emoji-shortcodes-replace]]
          [braid.emoji.client.styles :refer [autocomplete]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-autocomplete-engine! autocomplete-handler)
       (core/register-message-transform! emoji-ascii-replace)
       (core/register-message-transform! emoji-shortcodes-replace)
       (base/register-styles! autocomplete))))
