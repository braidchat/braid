(ns braid.bots.core
  (:require
   [braid.core.api :as core]
   [braid.bots.db :as db]
   [braid.bots.sync :as sync]
   [braid.bots.routes :as routes]))

(defn init! []
  #?(:cljs
     (do)

     :clj
     (do
       (core/regiter-db-schema! db/schema)
       (core/register-new-message-callback! sync/notify-bots!)
       (core/register-group-broadcast-hook! sync/group-change-broadcast!)
       (core/register-server-message-handlers! sync/server-message-handlers)
       (core/register-raw-http-handler! routes/bot-routes))))
