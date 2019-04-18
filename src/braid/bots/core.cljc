(ns braid.bots.core
  (:require
   [braid.core.api :as core]
   #?@(:clj
       [[braid.bots.server.db :as db]
        [braid.bots.server.sync :as sync]
        [braid.bots.server.routes :as routes]]
       :cljs
       [[braid.bots.client.autocomplete :as autocomplete]
        [braid.bots.client.events :as events]
        [braid.bots.client.views.bots-page :as views]
        [braid.bots.client.views.bots-page-styles :as styles]
        [braid.bots.client.subs :as subs]
        [braid.bots.client.remote-handlers :as remote-handlers]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-initial-user-data-handler!
         (fn [db data]
           (reduce (fn [db [group-id bots]]
                     (assoc-in db [:groups group-id :bots] bots))
                   db (::bots data))))
       (core/register-events! events/events)
       (core/register-subs! subs/subs)
       (core/register-autocomplete-engine! autocomplete/bots-autocomplete-engine)
       (core/register-group-page!
         {:key :bots
          :view views/bots-page-view})
       (core/register-styles!
         [:.app>.main styles/bot-page])
       (core/register-incoming-socket-message-handlers!
         remote-handlers/handlers))

     :clj
     (do
       (core/regiter-db-schema! db/schema)
       (core/register-new-message-callback! sync/notify-bots!)
       (core/register-group-broadcast-hook! sync/group-change-broadcast!)
       (core/register-server-message-handlers! sync/server-message-handlers)
       (core/register-raw-http-handler! routes/bot-routes)
       (core/register-initial-user-data!
         (fn [user-id] [::bots (db/bots-for-user-groups user-id)])))))
