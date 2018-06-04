(ns braid.custom-emoji.core
  "Allows group admins to create custom emoji"
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.custom-emoji.client.autocomplete :refer [autocomplete-handler
                                                          emoji-shortcodes-replace]]
          [braid.custom-emoji.client.styles :refer [settings-page]]
          [braid.custom-emoji.client.state :refer [initial-state
                                                   state-spec
                                                   initial-data-handler]]
          [braid.custom-emoji.client.views :refer [extra-emoji-settings-view]]]
         :clj
         [[braid.custom-emoji.server.db :refer [db-schema]]
          [braid.custom-emoji.server.core :refer [initial-user-data-fn
                                                  server-message-handlers]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-autocomplete-engine! autocomplete-handler)
       (core/register-message-transform! emoji-shortcodes-replace)
       (core/register-styles! settings-page)
       (core/register-state! initial-state state-spec)
       (core/register-initial-user-data-handler! initial-data-handler)
       (core/register-group-setting! extra-emoji-settings-view))

     :clj
     (do
       (core/register-db-schema! db-schema)
       (core/register-initial-user-data! initial-user-data-fn)
       (core/register-server-message-handler! server-message-handlers))))
