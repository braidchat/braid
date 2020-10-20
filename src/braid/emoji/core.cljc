(ns braid.emoji.core
  "Allows other modules to define emoji"
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:cljs
         [[braid.emoji.client.autocomplete :refer [autocomplete-handler]]
          [braid.emoji.client.text-replacements :refer [emoji-ascii-replace
                                                         emoji-shortcodes-replace]]
          [braid.emoji.client.styles :refer [autocomplete]]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-autocomplete-engine! autocomplete-handler)
       (chat/register-message-transform! emoji-ascii-replace)
       (chat/register-message-transform! emoji-shortcodes-replace)
       (base/register-styles! autocomplete))))
