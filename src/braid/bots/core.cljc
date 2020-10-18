(ns braid.bots.core
  (:require
   [braid.chat.api :as chat]
   [braid.base.api :as base]
   #?@(:clj
       [[braid.bots.server.db :as db]
        [braid.bots.server.sync :as sync]
        [braid.bots.server.routes :as routes]]
       :cljs
       [[braid.bots.client.autocomplete :as autocomplete]
        [braid.bots.client.events :as events]
        [braid.bots.client.views.bots-page :as views]
        [braid.bots.client.views.bots-page-styles :as styles]
        [braid.bots.client.views.bot-sender-view :as sender-view]
        [braid.core.client.routes :as routes]
        [braid.bots.client.subs :as subs]
        [braid.bots.client.remote-handlers :as remote-handlers]])))

(defn init! []
  #?(:cljs
     (do
       (chat/register-initial-user-data-handler!
         (fn [db data]
           ;; [TODO] to preserve same interface, putting bots inside groups
           ;; but probably could have that be a separate key
           (reduce (fn [db [group-id bots]]
                     (assoc-in db [:groups group-id :bots] bots))
                   db (::bots data))))
       (base/register-events! events/events)
       (base/register-subs! subs/subs)
       (chat/register-autocomplete-engine! autocomplete/bots-autocomplete-engine)
       (chat/register-admin-header-item!
         {:class "group-bots"
          :route-fn routes/group-page-path
          :route-args {:page-id "bots"}
          :body "Bots"})
       (chat/register-group-page!
         {:key :bots
          :view views/bots-page-view})
       (base/register-styles! styles/bots-page)
       (base/register-styles! styles/bot-notice)
       (base/register-incoming-socket-message-handlers!
         remote-handlers/handlers)
       (chat/register-message-sender-view! sender-view/sender-view))

     :clj
     (do
       (base/register-db-schema! db/schema)
       (chat/register-new-message-callback! sync/notify-bots!)
       (chat/register-group-broadcast-hook! sync/group-change-broadcast!)
       (base/register-server-message-handlers! sync/server-message-handlers)
       (doseq [route routes/bot-routes]
         (base/register-raw-http-handler! route))
       (chat/register-initial-user-data!
         (fn [user-id] {::bots (db/bots-for-user-groups user-id)}))
       (chat/register-anonymous-group-load!
         (fn [group-id info]
           (assoc-in info [:group :bots] (db/bots-in-group group-id)))))))
