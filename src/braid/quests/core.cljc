(ns braid.quests.core
  "Provides 'quests' for Braid - activies that users can complete to introduce them to the system"
  (:require
    [braid.core.api :as core]
    [braid.chat.api :as chat]
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.quests.client.views :refer [quests-header-view]]
          [braid.quests.client.styles :refer [quests-header]]
          [braid.quests.client.events :refer [events]]
          [braid.quests.client.helpers :as helpers]
          [braid.quests.client.core :refer [initial-data-handler
                                            event-listener
                                            initial-state
                                            schema]]]
         :clj
         [[braid.quests.server.core :refer [db-schema
                                            initial-user-data-fn
                                            server-message-handlers]]
          [braid.quests.server.db :refer [activate-first-quests-txn]]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-header-view! quests-header-view)
       (base/register-styles! quests-header)
       (chat/register-initial-user-data-handler! initial-data-handler)
       (base/register-event-listener! event-listener)
       (base/register-state! initial-state schema)
       (base/register-events! events)
       (base/register-subs!
         {:quests/active-quest-records
          (fn [state _]
            (helpers/get-active-quest-records state))}))

     :clj
     (do
       (base/register-db-schema! db-schema)
       (core/register-initial-user-data! initial-user-data-fn)
       (base/register-server-message-handlers! server-message-handlers)
       (chat/register-post-create-user-txn! activate-first-quests-txn))))
