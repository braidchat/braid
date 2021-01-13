(ns braid.emoji-custom.core
  "Allows group admins to create custom emoji"
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    #?@(:cljs
         [[braid.emoji.api :as emoji]
          [braid.emoji-custom.client.autocomplete :as autocomplete]
          [braid.emoji-custom.client.styles :refer [settings-page]]
          [braid.emoji-custom.client.state :as state]
          [braid.emoji-custom.client.views :refer [extra-emoji-settings-view]]]
         :clj
         [[braid.emoji-custom.server.db :refer [db-schema]]
          [braid.emoji-custom.server.core :refer [initial-user-data-fn
                                                  server-message-handlers]]])))

(defn init! []
  #?(:cljs
     (do
       (emoji/register-emoji!
         {:shortcode-lookup autocomplete/lookup})
       (base/register-styles! settings-page)
       (base/register-state! state/initial-state state/state-spec)
       (base/register-initial-user-data-handler! state/initial-data-handler)
       (chat/register-group-setting! extra-emoji-settings-view)
       (base/register-events! state/events)
       (base/register-subs! state/subscriptions)
       (base/register-incoming-socket-message-handlers!
         state/socket-message-handlers))

     :clj
     (do
       (base/register-db-schema! db-schema)
       (base/register-initial-user-data! initial-user-data-fn)
       (base/register-server-message-handlers! server-message-handlers))))
