(ns braid.emoji.core
  "Provides emoji autocomplete engine for Braid"
  (:require
    #?@(:cljs
         [[braid.core.core :as core]
          [braid.emoji.client.autocomplete :refer [autocomplete-handler]]
          [braid.emoji.client.text-replacements :refer [emoji-ascii-replace
                                                        emoji-shortcodes-replace
                                                        format-emojis]]
          [braid.emoji.client.styles :refer [emojione
                                             autocomplete
                                             settings-page]]
          [braid.emoji.client.core :refer [initial-state
                                           state-spec
                                           initial-data-handler]]
          [braid.emoji.client.views :refer [extra-emoji-settings-view]]]

         :clj
         [[braid.core.core :as core]
          [braid.emoji.server.db :refer [db-schema]]
          [braid.emoji.server.core :refer [initial-user-data-fn
                                           server-message-handlers]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-autocomplete-engine! autocomplete-handler)
       (core/register-message-transform! emoji-ascii-replace)
       (core/register-message-transform! emoji-shortcodes-replace)
       (core/register-message-formatter! format-emojis)
       (core/register-styles! emojione)
       (core/register-styles! autocomplete)
       (core/register-styles! settings-page)
       (core/register-state! initial-state state-spec)
       (core/register-initial-user-data-handler! initial-data-handler)
       (core/register-group-setting! extra-emoji-settings-view))

     :clj
     (do
       (core/register-db-schema! db-schema)
       (core/register-initial-user-data! initial-user-data-fn)
       (core/register-server-message-handler! server-message-handlers))))
