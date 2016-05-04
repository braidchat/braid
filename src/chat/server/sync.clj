(ns chat.server.sync
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [compojure.core :refer [GET POST routes defroutes context]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [taoensso.timbre :as timbre :refer [debugf]]
            [clojure.string :as string]
            [chat.server.db :as db]
            [chat.server.search :as search]
            [chat.server.invite :as invites]
            [chat.server.digest :as digest]
            [clojure.set :refer [difference intersection]]
            [chat.shared.util :refer [valid-nickname? valid-tag-name?]]
            [chat.server.extensions :refer [handle-thread-change]]))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter
                                  {:user-id-fn (fn [ob]
                                                 (get-in ob [:session :user-id]))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)
  (def chsk-send!                    send-fn)
  (def connected-uids                connected-uids))

(defroutes sync-routes
  (GET  "/chsk" req
      (-> req
          (assoc-in [:session :ring.middleware.anti-forgery/anti-forgery-token]
            *anti-forgery-token*)
          ring-ajax-get-or-ws-handshake))
  (POST "/chsk" req (ring-ajax-post req)))

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
            filtered-thread (update-in thread [:tag-ids] (partial filter user-tags))
            thread-with-last-opens (db/with-conn (db/thread-add-last-open-at filtered-thread uid))]
        (chsk-send! uid [:chat/thread thread-with-last-opens])))))

(defn broadcast-user-change
  "Broadcast user info change to clients that can see this user"
  [user-id info]
  (let [ids-to-send-to (intersection
                         (set (:any @connected-uids))
                         (into #{} (map :id)
                               (db/with-conn (db/fetch-users-for-user user-id))))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

(defn broadcast-group-change
  "Broadcast group change to clients that are in the group"
  [group-id info]
  (let [ids-to-send-to (intersection
                         (set (:any @connected-uids))
                         (into #{} (map :id)
                               (db/with-conn (db/get-users-in-group group-id))))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

(defmethod event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [event id ring-req]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (broadcast-user-change user-id [:user/connected user-id])))

(defmethod event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [event id ring-req]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (broadcast-user-change user-id [:user/disconnected user-id])))


(defn- user-can-message? [user-id ?data]
  (db/with-conn
    (every?
      true?
      (concat
        [(or (boolean (db/user-can-see-thread? user-id (?data :thread-id)))
             (do (timbre/warnf "User %s attempted to add message to disallowed thread %s"
                               user-id (?data :thread-id))
                 false))]
        (map
          (fn [tag-id]
            (or (boolean (db/user-in-tag-group? user-id tag-id))
                (do
                  (timbre/warnf "User %s attempted to add a disallowed tag %s" user-id
                                tag-id)
                  false)))
          (?data :mentioned-tag-ids))
        (map
          (fn [mentioned-id]
            (or (boolean (db/user-visible-to-user? user-id mentioned-id))
                (do (timbre/warnf "User %s attempted to mention disallowed user %s"
                                  user-id mentioned-id)
                    false)))
          (?data :mentioned-user-ids))))))

(defn notify-extensions
  "Notify extensions about a thread changing if they're interested"
  [msg]
  (let [exts (db/with-conn (db/extensions-watching (msg :thread-id)))]
    (doseq [ext exts]
      (handle-thread-change ext msg))))

(defmethod event-msg-handler :chat/new-message
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (when (user-can-message? user-id ?data)
      (db/with-conn (db/create-message!
                      (-> ?data
                          (update-in [:content] #(apply str (take 5000 %)))
                          (assoc :created-at (java.util.Date.)))))
      (broadcast-thread (?data :thread-id) [])
      (notify-extensions ?data)
      (when-let [cb ?reply-fn]
        (cb :braid/ok)))))

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

(defmethod event-msg-handler :user/set-password
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (string/blank? (?data :password))
      (when ?reply-fn (?reply-fn {:error "Password cannot be blank"}))
      (do
        (db/with-conn (db/set-user-password! user-id (?data :password)))
        (when ?reply-fn (?reply-fn {:ok true}))))))

(defmethod event-msg-handler :user/set-preferences
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn
      (doseq [[k v] ?data]
        (db/user-set-preference! user-id k v)))
    (when ?reply-fn (?reply-fn :braid/ok))))

(defmethod event-msg-handler :chat/hide-thread
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/user-hide-thread! user-id ?data))))

(defmethod event-msg-handler :chat/mark-thread-read
  [{:as ev-msg :keys [ring-req ?data]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn (db/update-thread-last-open ?data user-id))))

(defmethod event-msg-handler :chat/create-tag
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (if (db/with-conn (db/user-in-group? user-id (?data :group-id)))
      (if (valid-tag-name? (?data :name))
        (let [new-tag (db/with-conn (db/create-tag! (select-keys ?data [:id :name :group-id])))]
          (db/with-conn
            (doseq [uid (->> (:any @connected-uids)
                             (filter #(db/user-in-tag-group? % (:id new-tag)))
                             (remove (partial = user-id)))]
              (chsk-send! uid [:chat/create-tag ?data])))
          (when ?reply-fn
            (?reply-fn {:ok true})))
        (do (timbre/warnf "User %s attempted to create a tag %s with an invalid name"
                          user-id (?data :name))
            (when ?reply-fn
              (?reply-fn {:error "invalid tag name"}))))
      ; TODO: indicate permissions error to user?
      (timbre/warnf "User %s attempted to create a tag %s in a disallowed group"
                    user-id (?data :name) (?data :group-id)))))

(defmethod event-msg-handler :chat/set-tag-description
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [{:keys [tag-id description]} ?data]
      (when (and tag-id description)
        (db/with-conn
          (let [group-id (db/tag-group-id tag-id)]
            (when (db/user-is-group-admin? user-id group-id)
              (db/tag-set-description! tag-id description)
              (broadcast-group-change
                group-id [:group/tag-descrption-change [tag-id description]]))))))))

(defmethod event-msg-handler :chat/create-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn
      (if-not (db/group-exists? (?data :name))
        (let [new-group (db/create-group! ?data)]
          (db/user-make-group-admin! user-id (new-group :id)))
        (do
          (timbre/warnf "User %s attempted to create group that already exsits %s"
                        user-id (?data :name))
          (when ?reply-fn
            (?reply-fn {:error "Group name already taken"})))))))

(defmethod event-msg-handler :chat/search
  [{:keys [event id ?data ring-req ?reply-fn send-fn] :as ev-msg}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn
      (let [user-tags (db/get-user-visible-tag-ids user-id)
            filter-tags (fn [t] (update-in t [:tag-ids] (partial filter user-tags)))
            thread-ids (search/search-threads-as user-id ?data)
            threads (map (comp filter-tags db/get-thread) (take 25 thread-ids))]
        (when ?reply-fn
          (?reply-fn {:threads threads :thread-ids thread-ids}))))))

(defmethod event-msg-handler :chat/load-threads
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (db/with-conn
      (let [user-tags (db/get-user-visible-tag-ids user-id)
            filter-tags (fn [t] (update-in t [:tag-ids] (partial filter user-tags)))
            thread-ids (filter (partial db/user-can-see-thread? user-id) ?data)
            threads (map filter-tags (db/get-threads thread-ids))]
        (when ?reply-fn
          (?reply-fn {:threads threads}))))))

(defmethod event-msg-handler :chat/threads-for-tag
  [{:keys [event id ?data ring-req ?reply-fn send-fn] :as ev-msg}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [user-tags (db/with-conn (db/get-user-visible-tag-ids user-id))
          filter-tags (fn [t] (update-in t [:tag-ids] (partial filter user-tags)))
          offset (get ?data :offset 0)
          limit (get ?data :limit 50)
          threads (db/with-conn (-> (db/threads-with-tag user-id (?data :tag-id) offset limit)
                                     (update :threads (comp doall (partial map filter-tags)))))]
      (when ?reply-fn
        (?reply-fn threads)))))

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

(defmethod event-msg-handler :chat/make-user-admin
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [{new-admin-id :user-id group-id :group-id} ?data]
      (db/with-conn
        (when (and new-admin-id group-id
                (db/user-is-group-admin? user-id group-id))
          (db/user-make-group-admin! new-admin-id group-id)
          (broadcast-group-change group-id [:group/new-admin [group-id new-admin-id]]))))))

(defmethod event-msg-handler :session/start
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [user-id (get-in ring-req [:session :user-id])]
    (let [connected (set (:any @connected-uids))]
      (chsk-send! user-id [:session/init-data
                           (db/with-conn
                             {:user-id user-id
                              :version-checksum (digest/from-file "public/js/desktop/out/braid.js")
                              :user-nickname (db/get-nickname user-id)
                              :user-groups (db/get-groups-for-user user-id)
                              :user-threads (db/get-open-threads-for-user user-id)
                              :user-subscribed-tag-ids (db/get-user-subscribed-tag-ids user-id)
                              :user-preferences (db/get-user-preferences user-id)
                              :users (into ()
                                           (map #(assoc % :status
                                                   (if (connected (% :id)) :online :offline)))
                                           (db/fetch-users-for-user user-id))
                              :invitations (db/fetch-invitations-for-user user-id)
                              :tags (db/fetch-tags-for-user user-id)})]))))

(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))
