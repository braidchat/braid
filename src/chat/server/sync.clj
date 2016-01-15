(ns chat.server.sync
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [compojure.core :refer [GET POST routes defroutes context]]
            [taoensso.timbre :as timbre :refer [debugf]]
            [clojure.core.async :as async :refer [<! <!! >! >!! put! chan go go-loop]]
            [chat.server.db :as db]
            [chat.server.search :as search]
            [chat.server.invite :as invites]
            [clojure.set :refer [difference intersection]]
            [chat.server.util :refer [valid-nickname?]]))

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
        thread (db/with-conn (db/get-thread thread-id))]
    (doseq [uid user-ids-to-send-to]
      (let [user-tags (db/with-conn (db/get-user-visible-tag-ids uid))
            filtered-thread (update-in thread [:tag-ids] (partial filter user-tags))]
        (chsk-send! uid [:chat/thread filtered-thread])))))

(defn broadcast-user-change
  "Broadcast user info change to clients that can see this user"
  [user-id info]
  (let [ids-to-send-to (map :id (db/with-conn (db/fetch-users-for-user user-id)))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

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

(defmethod event-msg-handler :thread/add-mention
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  ; TODO: verify that all required keys are present?
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [{:keys [thread-id mentioned-id]} ?data]
      (if (db/with-conn (db/user-visible-to-user? user-id mentioned-id))
        (do (db/with-conn (db/thread-add-mentioned! thread-id mentioned-id))
            (let [tags (db/with-conn (db/get-user-visible-tag-ids mentioned-id))
                  thread (db/with-conn (-> (db/get-thread thread-id)
                                           (update-in [:tag-ids] (partial filter tags))))]
              (chsk-send! mentioned-id [:chat/thread thread])))
        ; TODO: indicate permissions error to user?
        (timbre/warnf "User %s attempted to mention disallowed user %s" user-id mentioned-id)))))

(defmethod event-msg-handler :user/subscribe-to-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-subscribe-to-tag! user-id ?data))))

(defmethod event-msg-handler :user/unsubscribe-from-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-unsubscribe-from-tag! user-id ?data))))

(defmethod event-msg-handler :user/set-nickname
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (valid-nickname? (?data :nickname))
      (try
        (do (db/with-conn @(db/set-nickname! user-id (?data :nickname)))
            (broadcast-user-change user-id [:user/name-change {:user-id user-id :nickname (?data :nickname)}])
            (when ?reply-fn (?reply-fn {:ok true})))
        (catch java.util.concurrent.ExecutionException _
          (when ?reply-fn (?reply-fn {:error "Nickname taken"}))))
      (when ?reply-fn (?reply-fn {:error "Invalid nickname"})))))

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
    (db/with-conn
      (if-not (db/group-exists? (?data :name))
        (let [new-group (db/create-group! ?data)]
          (db/user-add-to-group! user-id (new-group :id)))
        (do
          (timbre/warnf "User %s attempted to create group that already exsits %s"
                        user-id (?data :name))
          (when ?reply-fn
            (?reply-fn {:error "Group name already taken"})))))))

(defmethod event-msg-handler :chat/search
  [{:keys [event id ?data ring-req ?reply-fn send-fn] :as ev-msg}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [user-tags (db/with-conn (db/get-user-visible-tag-ids user-id))
          filter-tags (fn [t] (update-in t [:tag-ids] (partial filter user-tags)))
          threads (db/with-conn (->> (search/search-threads-as user-id ?data)
                                     (map (comp filter-tags db/get-thread))
                                     doall))]
      (when ?reply-fn
        (?reply-fn {:threads threads})))))

(defmethod event-msg-handler :chat/invite-to-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-in-group? user-id (?data :group-id)))
      (let [data (assoc ?data :inviter-id user-id)
            invitation (db/with-conn (db/create-invitation! data))]
        (if-let [invited-user (db/with-conn (db/user-with-email (invitation :invitee-email)))]
          (chsk-send! (invited-user :id) [:chat/invitation-recieved invitation])
          (invites/send-invite invitation)))
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
        (chsk-send! user-id [:chat/joined-group
                             {:group (db/get-group (invite :group-id))
                              :tags (db/get-group-tags (invite :group-id))}])
        (chsk-send! user-id [:chat/update-users (db/fetch-users-for-user user-id)])
        (let [other-users (->> (db/get-users-in-group (invite :group-id))
                               (into [] (comp (remove (partial = user-id)) (map :id))))
              new-user (db/user-by-id user-id)]
          (doseq [other-id other-users]
            (chsk-send! other-id [:chat/new-user new-user]))))
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
                            :user-nickname (db/get-nickname user-id)
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
