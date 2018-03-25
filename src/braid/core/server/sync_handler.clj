(ns braid.core.server.sync-handler
  (:require
   [braid.core.module-helpers :refer [defhook]]
   [braid.core.server.db :as db]
   [braid.core.server.socket :as socket]
   [braid.core.server.sync-helpers :as helpers]
   [mount.core :refer [defstate]]
   [schema.core :as s]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre :refer [debugf]]))

(defhook
  :writer register-server-message-handlers!
  :reader message-handlers
  :initial-value {}
  :add-fn merge)

(defn run-cofx! [ev-msg cofx]
  (when-let [args (cofx :chsk-send!)]
    (apply socket/chsk-send! args))
  (when-let [txns (cofx :db-run-txns!)]
    (db/run-txns! txns))
  (when-let [[group info] (cofx :group-broadcast!)]
    (helpers/broadcast-group-change group info))
  (when-let [info (cofx :reply!)]
    (when-let [reply-fn (:?reply-fn ev-msg)]
      (reply-fn info))))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (when-not (= event [:chsk/ws-ping])
    (when (not= event :braid.client/ping)
      (debugf "User: %s Event: %s" (get-in ev-msg [:ring-req :session :user-id]) event))

    (when-let [user-id (get-in ev-msg [:ring-req :session :user-id])]
      (if-let [dynamic-msg-handler (get @message-handlers id)]
        (run-cofx! ev-msg (dynamic-msg-handler (assoc ev-msg :user-id user-id)))
        (event-msg-handler (assoc ev-msg :user-id user-id))))))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn user-id]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :braid.client/ping
  [{:as ev-msg :keys [?reply-fn]}]
  (when-let [reply ?reply-fn]
    (reply [:braid.server/pong])))

(defstate router
  :start (sente/start-chsk-router! socket/ch-chsk event-msg-handler*)
  :stop (router))
