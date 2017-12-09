(ns braid.server.sync-handler
  (:require
    [mount.core :refer [defstate]]
    [schema.core :as s]
    [taoensso.sente :as sente]
    [taoensso.timbre :as timbre :refer [debugf]]
    [braid.core.hooks :as hooks]
    [braid.server.db :as db]
    [braid.server.socket :as socket]))

(defn run-cofx! [cofx]
  (when-let [args (cofx :chsk-send!)]
    (apply socket/chsk-send! args))
  (when-let [txns (cofx :db-run-txns!)]
    (db/run-txns! txns)))

(defmulti event-msg-handler :id)

(hooks/def-override-hook message-handler :id
  [evt]
  ;; Result of this hook will be passed to run-cofx!
  ;; for default case, assume that it directly causes side-effects
  {:unhandled (event-msg-handler evt)})

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (when-not (= event [:chsk/ws-ping])
    (debugf "User: %s Event: %s" (get-in ev-msg [:ring-req :session :user-id]) event)

    (when-let [user-id (get-in ev-msg [:ring-req :session :user-id])]
      (run-cofx! (message-handler (assoc ev-msg :user-id user-id))))))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn user-id]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defstate router
  :start (sente/start-chsk-router! socket/ch-chsk event-msg-handler*)
  :stop (router))
