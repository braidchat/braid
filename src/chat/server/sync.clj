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
  (when-not (= event [:chsk/ws-ping]) (debugf "Event: %s" event))
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/ws-ping
  [ev-msg]
  ; Do nothing, just avoid unhandled event message
  )

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

(defmethod event-msg-handler :chat/new-message
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-can-see-thread? user-id (?data :thread-id)))
      (do (db/with-conn (db/create-message! ?data))
        (broadcast-thread (?data :thread-id) [user-id]))
      ; TODO: indicate permissions error to user?
      (timbre/warnf "User %s attempted to add message to disallowed thread %s"
                    user-id (?data :thread-id)))))

(defmethod event-msg-handler :thread/add-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-in-tag-group? user-id (?data :tag-id)))
      (do (db/with-conn (db/thread-add-tag! (?data :thread-id) (?data :tag-id)))
          (broadcast-thread (?data :thread-id) [user-id]))
      ; TODO: indicate permissions error to user?
      (timbre/warnf "User %s attempted to add a disallowed tag %s" user-id
                    (?data :tag-id)))))

(defmethod event-msg-handler :user/subscribe-to-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-subscribe-to-tag! user-id ?data))))

(defmethod event-msg-handler :user/unsubscribe-from-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-unsubscribe-from-tag! user-id ?data))))

(defmethod event-msg-handler :chat/hide-thread
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (db/with-conn (db/user-hide-thread! (get-in ring-req [:session :user-id]) ?data)))

(defmethod event-msg-handler :chat/create-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-in-group? user-id (?data :group-id)))
      (let [new-tag (db/with-conn (db/create-tag! (select-keys ?data [:id :name :group-id])))]
        (db/with-conn
          (doseq [uid (->> (:any @connected-uids)
                           (filter #(db/user-in-tag-group? % (:id new-tag)))
                           (remove (partial = user-id)))]
            (chsk-send! uid [:chat/create-tag ?data]))))
      ; TODO: indicate permissions error to user?
      (timbre/warnf "User %s attempted to create a tag %s in a disallowed group"
                    user-id (?data :name) (?data :group-id)))))

(defmethod event-msg-handler :chat/create-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (println "Creating group" ?data)
    (db/with-conn
      (if-not (db/group-exists? (?data :name))
        (let [new-group (db/create-group! ?data)]
          (db/user-add-to-group! user-id (new-group :id)))
        (do
          (timbre/warnf "User %s attempted to create group that already exsits %s"
                        user-id (?data :name))
          (when ?reply-fn
            (println "sending reply")
            (?reply-fn {:error "Group name already taken"})))))))

(defmethod event-msg-handler :chat/invite-to-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-in-group? user-id (?data :group-id)))
      (let [data (assoc ?data :inviter-id user-id)
            invitation (db/with-conn (db/create-invitation! data))]
        (when-let [invited-user (db/with-conn (db/user-with-email (invitation :to)))]
          (chsk-send! (invited-user :id) [:chat/invitation-recieved invitation])))
      ; TODO: indicate permissions error to user?
      (timbre/warnf "User %s attempted to invite %s to a group %s they don't have access to"
                    user-id (?data :invitee-email) (?data :group-id)))))

(defmethod event-msg-handler :chat/invitation-accept
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if-let [invite (db/with-conn (db/get-invite (?data :id)))]
      (db/with-conn
        (db/user-add-to-group! user-id (invite :group-id))
        (db/user-subscribe-to-group-tags! user-id (invite :group-id))
        (db/retract-invitation! (invite :id))
        ; TODO: update users visible to user
        (chsk-send! user-id [:chat/joined-group (db/get-group (invite :group-id))]))
      (timbre/warnf "User %s attempted to accept nonexistant invitaiton %s"
                    user-id (?data :id)))))

(defmethod event-msg-handler :chat/invitation-decline
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if-let [invite (db/with-conn (db/get-invite (?data :id)))]
      (db/with-conn (db/retract-invitation! (invite :id)))
      (timbre/warnf "User %s attempted to decline nonexistant invitaiton %s"
                    user-id (?data :id)))))

(defmethod event-msg-handler :session/start
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (chsk-send! user-id [:session/init-data
                         (db/with-conn
                           {:user-id user-id
                            :user-groups (db/get-groups-for-user user-id)
                            :user-threads (db/get-open-threads-for-user user-id)
                            :user-subscribed-tag-ids (db/get-user-subscribed-tag-ids user-id)
                            :users (db/fetch-users-for-user user-id)
                            :invitations (db/fetch-invitations-for-user user-id)
                            :tags (db/fetch-tags-for-user user-id)})])))

(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))
