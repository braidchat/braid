(ns braid.custom-emoji.core
  "Allows group admins to create custom emoji"
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
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
       (base/register-styles! settings-page)
       (base/register-state! state/initial-state state/state-spec)
       (chat/register-initial-user-data-handler! state/initial-data-handler)
       (chat/register-group-setting! extra-emoji-settings-view)
       (base/register-events! state/events)
       (base/register-subs! state/subscriptions)
       (base/register-incoming-socket-message-handlers!
         state/socket-message-handlers))

     :clj
     (do
       (base/register-db-schema! db-schema)
       (chat/register-initial-user-data! initial-user-data-fn)
       (base/register-server-message-handlers! server-message-handlers))))
