(ns chat.server.sync
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [compojure.core :refer [GET POST routes defroutes context]]
            [taoensso.timbre :as timbre :refer [debugf]]
            [clojure.core.async :as async :refer [<! <!! >! >!! put! chan go go-loop]]
            [chat.server.db :as db]
            [clojure.set :refer [difference intersection]]))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn (fn [ob]
                                                                          (get-in ob [:session :user-id]))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)
  (def chsk-send!                    send-fn)
  (def connected-uids                connected-uids))

(defroutes sync-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req)))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defn broadcast-thread
  "broadcasts thread to all subscribed users, except those in ids-to-skip"
  [thread-id ids-to-skip]
  (let [subscribed-user-ids (db/with-conn
                              (db/get-users-subscribed-to-thread thread-id))
        user-ids-to-send-to (-> (difference
                                  (intersection
                                    (set subscribed-user-ids)
                                    (set (:any @connected-uids)))
                                  (set ids-to-skip)))
        thread (db/with-conn
                 (db/get-thread thread-id))]
    (doseq [uid user-ids-to-send-to]
      (chsk-send! uid [:chat/thread thread]))))

(defmethod event-msg-handler :chsk/ws-ping
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when ?reply-fn
    (?reply-fn [:chsk/ws-pong])))

(defmethod event-msg-handler :chat/new-message
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/create-message! ?data))
    (when ?reply-fn
      (?reply-fn [:chsk/okay]))
    (broadcast-thread (?data :thread-id) [user-id])))

(defmethod event-msg-handler :thread/add-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/thread-add-tag! (?data :thread-id) (?data :tag-id)))
    (when ?reply-fn
      (?reply-fn [:chsk/okay]))
    (broadcast-thread (?data :thread-id) [user-id])))

(defmethod event-msg-handler :user/subscribe-to-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-subscribe-to-tag! user-id ?data))
    (when ?reply-fn
      (?reply-fn [:chsk/okay]))))

(defmethod event-msg-handler :user/unsubscribe-from-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-unsubscribe-from-tag! user-id ?data))
    (when ?reply-fn
      (?reply-fn [:chsk/okay]))))

(defmethod event-msg-handler :chat/hide-thread
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (db/with-conn (db/user-hide-thread! (get-in ring-req [:session :user-id]) ?data))
  (when ?reply-fn
    (?reply-fn [:chsk/okay])))

(defmethod event-msg-handler :chat/create-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (db/with-conn (db/create-tag! {:id (?data :id)
                                 :name (?data :name)}))
  (when ?reply-fn
    (?reply-fn [:chsk/okay]))
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (doseq [uid (->> (:any @connected-uids)
                     (remove (partial = user-id)))]
      (chsk-send! uid [:chat/create-tag ?data]))))

(defmethod event-msg-handler :session/start
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (chsk-send! user-id [:session/init-data
                         (db/with-conn
                           {:user-id user-id
                            :user-threads (db/get-open-threads-for-user user-id)
                            :user-subscribed-tag-ids (db/get-user-subscribed-tag-ids user-id)
                            :users (db/fetch-users)
                            :tags (db/fetch-tags)})])))

(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))
