(ns braid.core.server.sync-handler
  (:require
   [braid.core.hooks :as hooks]
   [braid.core.server.db :as db]
   [braid.core.server.socket :as socket]
   [braid.core.server.sync-helpers :as helpers]
   [mount.core :refer [defstate]]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre :refer [debugf]]
   [braid.core.server.db.group :as group]
   [braid.core.server.db.thread :as thread]))

(defonce message-handlers
  (hooks/register! (atom {}) {keyword? fn?}))

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

(defmulti event-msg-handler :id)

(defmulti anon-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (when-not (= event [:chsk/ws-ping])
    (when-not (= event [:braid.client/ping])
      (debugf "User: %s Event: %s" (get-in ev-msg [:ring-req :session :user-id]) event))

    (if-let [user-id (get-in ev-msg [:ring-req :session :user-id])]
      (if-let [dynamic-msg-handler (get @message-handlers id)]
        (run-cofx! ev-msg (dynamic-msg-handler (assoc ev-msg :user-id user-id)))
        (event-msg-handler (assoc ev-msg :user-id user-id)))
      (anon-msg-handler ev-msg))))

;; anonymous event handlers

(defmethod anon-msg-handler :default
  [{:as ev-msg :keys [?reply-fn event]}]
  (debugf "Unhandled anon event %s" (:id ev-msg))
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(defmethod anon-msg-handler :braid.server.anon/load-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [group (group/group-by-id ?data)]
    (when (:public? group)
      (?reply-fn {:tags (group/group-tags ?data)
                  :group group
                  :threads (thread/public-threads ?data)})
      (helpers/add-anonymous-reader ?data (get-in ev-msg [:ring-req :session :fake-id])))))

(defmethod anon-msg-handler :chsk/uidport-close
  [ev-msg]
  (debugf "Closing connection for anonymous client %s" (:client-id ev-msg))
  (helpers/remove-anonymous-reader (get-in ev-msg [:ring-req :session :fake-id])))

(defmethod anon-msg-handler :braid.client/ping
  [{:as ev-msg :keys [?reply-fn]}]
  (when-let [reply ?reply-fn]
    (reply [:braid.server/pong])))

;; logged in event handlers

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
