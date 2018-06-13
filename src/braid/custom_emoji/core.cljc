(ns braid.custom-emoji.core
  "Allows group admins to create custom emoji"
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[braid.emoji.api :as emoji]
          [braid.custom-emoji.client.autocomplete :as autocomplete]
          [braid.custom-emoji.client.styles :refer [settings-page]]
          [braid.custom-emoji.client.state :as state]
          [braid.custom-emoji.client.views :refer [extra-emoji-settings-view]]]
         :clj
         [[braid.custom-emoji.server.db :refer [db-schema]]
          [braid.custom-emoji.server.core :refer [initial-user-data-fn
                                                  server-message-handlers]]])))

(defn init! []
  #?(:cljs
     (do
       (emoji/register-emoji!
         {:shortcode-lookup autocomplete/lookup})
       (core/register-styles! settings-page)
       (core/register-state! state/initial-state state/state-spec)
       (core/register-initial-user-data-handler! state/initial-data-handler)
       (core/register-group-setting! extra-emoji-settings-view)
       (core/register-events! state/events)
       (core/register-subs! state/subscriptions)
       (core/register-incoming-socket-message-handlers!
         state/socket-message-handlers))

     :clj
     (do
       (core/register-db-schema! db-schema)
       (core/register-initial-user-data! initial-user-data-fn)
       (core/register-server-message-handlers! server-message-handlers))))
