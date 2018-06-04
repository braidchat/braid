(ns braid.emoji.core
  "Provides emoji autocomplete engine for Braid"
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.emoji.client.autocomplete :refer [autocomplete-handler]]
          [braid.emoji.client.text-replacements :refer [emoji-ascii-replace
                                                        emoji-shortcodes-replace
                                                        format-emojis]]
          [braid.emoji.client.styles :refer [emojione
                                             autocomplete]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-autocomplete-engine! autocomplete-handler)
       (core/register-message-transform! emoji-ascii-replace)
       (core/register-message-transform! emoji-shortcodes-replace)
       (core/register-message-formatter! format-emojis)
       (core/register-styles! emojione)
       (core/register-styles! autocomplete))))
