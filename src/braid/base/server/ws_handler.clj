(ns braid.base.server.ws-handler
  (:require
    [braid.base.server.socket :as socket]
    [braid.core.server.sync-helpers :as helpers]
    [braid.core.server.db :as db]
    [braid.core.hooks :as hooks]
    [mount.core :refer [defstate]]
    [taoensso.sente :as sente]
    [taoensso.timbre :as timbre :refer [debugf]]))

(defonce message-handlers
  (hooks/register! (atom {}) {keyword? fn?}))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn user-id]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/ws-ping
  [ev-msg]
  ;; Do nothing, just avoid unhandled event message
  )

(defmulti anon-msg-handler :id)

(defmethod anon-msg-handler :default
  [{:as ev-msg :keys [?reply-fn event]}]
  (debugf "Unhandled anon event %s" (:id ev-msg))
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(defn run-cofx! [ev-msg cofx]
  (when-let [args (:chsk-send! cofx)]
    (apply socket/chsk-send! args))
  (when-let [txns (:db-run-txns! cofx)]
    (db/run-txns! txns))
  (when-let [[group info] (:group-broadcast! cofx)]
    (helpers/broadcast-group-change group info))
  (when-let [info (:reply! cofx)]
    (when-let [reply-fn (:?reply-fn ev-msg)]
      (reply-fn info))))

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (when-not (= event [:chsk/ws-ping])
    (when-not (= event [:braid.client/ping])
      (debugf "User: %s Event: %s" (get-in ev-msg [:ring-req :session :user-id]) event))

    (if-let [user-id (get-in ev-msg [:ring-req :session :user-id])]
      (if-let [dynamic-msg-handler (get @message-handlers id)]
        (run-cofx! ev-msg (dynamic-msg-handler (assoc ev-msg :user-id user-id)))
        (event-msg-handler (assoc ev-msg :user-id user-id)))
      (anon-msg-handler ev-msg))))

(defstate router
  :start (sente/start-chsk-router! socket/ch-chsk event-msg-handler*)
  :stop (router))
