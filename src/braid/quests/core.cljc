(ns braid.quests.core
  "Provides 'quests' for Braid - activies that users can complete to introduce them to the system"
  (:require
    #?@(:cljs
         [[braid.core.core :as core]
          [braid.quests.client.views :refer [quests-header-view]]
          [braid.quests.client.styles :refer [quests-header]]
          [braid.quests.client.core :refer [initial-data-handler
                                            event-listener
                                            state]]]
         :clj
         [[braid.core.core :as core]
          [braid.quests.server.core :refer [db-schema
                                            initial-user-data-fn
                                            server-message-handlers]]
          [braid.quests.server.db :refer [activate-first-quests-txn]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-header-view! quests-header-view)
       (core/register-styles! quests-header)
       (core/register-initial-user-data-handler! initial-data-handler)
       (core/register-event-listener! event-listener)
       (core/register-state! state))

     :clj
     (do
       (core/register-db-schema! db-schema)
       (core/register-initial-user-data! initial-user-data-fn)
       (core/register-server-message-handler! server-message-handlers)
       (core/register-post-create-user-txn! activate-first-quests-txn))))
