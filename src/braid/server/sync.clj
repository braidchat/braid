(ns braid.server.sync
  (:require
    [braid.common.schema :refer [new-message-valid? upload-valid?]]
    [braid.common.util :as util :refer [valid-nickname? valid-tag-name?]]
    [braid.server.bots :as bots]
    [braid.server.db :as db]
    [braid.server.db.bot :as bot]
    [braid.server.db.common :refer [bot->display]]
    [braid.server.db.group :as group]
    [braid.server.db.invitation :as invitation]
    [braid.server.db.message :as message]
    [braid.server.db.tag :as tag]
    [braid.server.db.thread :as thread]
    [braid.server.db.upload :as upload]
    [braid.server.db.user :as user]
    [braid.server.digest :as digest]
    [braid.server.events :as events]
    [braid.server.invite :as invites]
    [braid.server.message-format :refer [parse-tags-and-mentions]]
    [braid.server.quests.db :as quests]
    [braid.server.quests.sync]
    [braid.server.search :as search]
    [braid.server.socket :refer [chsk-send! connected-uids]]
    [braid.server.sync-handler :refer [event-msg-handler]]
    [braid.server.sync-helpers :refer [notify-users]]
    [braid.server.util :refer [valid-url?]]
    [clojure.set :refer [difference intersection]]
    [clojure.string :as string]
    [environ.core :refer [env]]
    [taoensso.timbre :as timbre :refer [debugf]]
    [taoensso.truss :refer [have]]))

(defn broadcast-thread
  "broadcasts thread to all users with the thread open, except those in ids-to-skip"
  [thread-id ids-to-skip]
  (let [user-ids (-> (difference
                       (intersection
                         (set (thread/users-with-thread-open thread-id))
                         (set (:any @connected-uids)))
                       (set ids-to-skip)))
        thread (thread/thread-by-id thread-id)]
    (doseq [uid user-ids]
      (let [user-tags (tag/tag-ids-for-user uid)
            filtered-thread (update-in thread [:tag-ids]
                                       (partial into #{} (filter user-tags)))
            thread-with-last-opens (thread/thread-add-last-open-at
                                     filtered-thread uid)]
        (chsk-send! uid [:braid.client/thread thread-with-last-opens])))))

(defn broadcast-user-change
  "Broadcast user info change to clients that can see this user"
  [user-id info]
  (let [ids-to-send-to (disj
                         (intersection
                           (set (:any @connected-uids))
                           (into
                             #{} (map :id)
                             (user/users-for-user user-id)))
                         user-id)]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

(defn broadcast-group-change
  "Broadcast group change to clients that are in the group"
  [group-id info]
  (let [ids-to-send-to (intersection
                         (set (:any @connected-uids))
                         (into #{} (map :id) (group/group-users group-id)))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info)))
  (doseq [bot (bot/bots-for-event group-id)]
    (bots/send-event-notification bot info)))

; TODO: when using clojure.spec, use spec to validate this
(defn user-can-message? [user-id ?data]
  ; TODO: also check that thread in group
  (every?
      true?
      (concat
        [(or (boolean (thread/user-can-see-thread? user-id (?data :thread-id)))
             (do (timbre/warnf
                   "User %s attempted to add message to disallowed thread %s"
                   user-id (?data :thread-id))
                 false))
         (or (boolean (if-let [cur-group (thread/thread-group-id (?data :thread-id))]
                        (= (?data :group-id) cur-group)
                        true)))]
        (map
          (fn [tag-id]
            (and
              (or (boolean (= (?data :group-id) (tag/tag-group-id tag-id)))
                  (do
                    (timbre/warnf
                      "User %s attempted to add a tag %s from a different group"
                      user-id tag-id)
                    false))
              (or (boolean (tag/user-in-tag-group? user-id tag-id))
                  (do
                    (timbre/warnf "User %s attempted to add a disallowed tag %s"
                                  user-id tag-id)
                    false))))
          (?data :mentioned-tag-ids))
        (map
          (fn [mentioned-id]
            (and
              (or (boolean (group/user-in-group? user-id (?data :group-id)))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                      false))
              (or (boolean (user/user-visible-to-user? user-id mentioned-id))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                    false))))
          (?data :mentioned-user-ids)))))


(defn notify-bots [new-message]
  ; Notify bots mentioned in the message
  (when-let [bot-name (second (re-find #"^/(\w+)\b" (:content new-message)))]
    (when-let [bot (bot/bot-by-name-in-group bot-name (new-message :group-id))]
      (timbre/debugf "notifying bot %s" bot)
      (future (bots/send-message-notification bot new-message))))
  ; Notify bots subscribed to the thread
  (doseq [bot (bot/bots-watching-thread (new-message :thread-id))]
    (timbre/debugf "notifying bot %s" bot)
    (future (bots/send-message-notification bot new-message)))
  ; Notify bots subscribed to all messages
  (doseq [bot (bot/bots-for-message (new-message :group-id))]
    (when (thread/thread-has-tags? (new-message :thread-id))
      (future (bots/send-message-notification bot new-message)))))

(defn broadcast-new-user-to-group
  [user-id group-id]
  (broadcast-group-change
    group-id
    [:braid.client/new-user [(user/user-by-id user-id) group-id]]))


;; Handlers

(defmethod event-msg-handler :chsk/ws-ping
  [ev-msg]
  ; Do nothing, just avoid unhandled event message
  )

(defmethod event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [user-id]}]
  (broadcast-user-change user-id [:braid.client/user-connected user-id]))

(defmethod event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [user-id]}]
  (broadcast-user-change user-id [:braid.client/user-disconnected user-id]))

(defmethod event-msg-handler :braid.server/new-message
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [new-message (-> ?data
                        (update :mentioned-tag-ids vec)
                        (update :mentioned-user-ids vec)
                        (update-in [:content] #(apply str (take 5000 %)))
                        (assoc :created-at (java.util.Date.)))]
    (if (new-message-valid? new-message)
      (when (user-can-message? user-id new-message)
        (db/run-txns! (message/create-message-txn new-message))
        (when-let [cb ?reply-fn]
          (cb :braid/ok))
        (broadcast-thread (new-message :thread-id) [])
        (notify-users new-message)
        (notify-bots new-message))
      (do
        (timbre/warnf "Malformed new message: %s" (pr-str new-message))
        (when-let [cb ?reply-fn]
          (cb :braid/error))))))

(defmethod event-msg-handler :braid.server/tag-thread
  [{:as ev-msg :keys [?data user-id]}]
  (let [{:keys [thread-id tag-id]} ?data]
    (let [group-id (tag/tag-group-id tag-id)]
      (db/run-txns! (thread/tag-thread-txn group-id thread-id tag-id))
      (broadcast-thread thread-id []))
    ; TODO do we need to notify-users and notify-bots
    ))

(defmethod event-msg-handler :braid.server/subscribe-to-tag
  [{:as ev-msg :keys [?data user-id]}]
  (db/run-txns! (tag/user-subscribe-to-tag-txn user-id ?data)))

(defmethod event-msg-handler :braid.server/unsubscribe-from-tag
  [{:as ev-msg :keys [?data user-id]}]
  (db/run-txns! (tag/user-unsubscribe-from-tag-txn user-id ?data)))

(defmethod event-msg-handler :braid.server/set-nickname
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (valid-nickname? (?data :nickname))
    (try
      (do (db/run-txns! (user/set-nickname-txn user-id (?data :nickname)))
          (broadcast-user-change user-id [:braid.client/name-change
                                          {:user-id user-id
                                           :nickname (?data :nickname)}])
          (when ?reply-fn (?reply-fn {:ok true})))
      (catch java.util.concurrent.ExecutionException _
        (when ?reply-fn (?reply-fn {:error "Nickname taken"}))))
    (when ?reply-fn (?reply-fn {:error "Invalid nickname"}))))

(defmethod event-msg-handler :braid.server/set-user-avatar
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (valid-url? ?data)
    (do (db/run-txns! (user/set-user-avatar-txn user-id ?data))
        (broadcast-user-change user-id [:braid.client/user-new-avatar
                                        {:user-id user-id
                                         :avatar ?data}])
        (when-let [r ?reply-fn] (r {:braid/ok true})))
    (do (timbre/warnf "Couldn't set user avatar to %s" ?data)
        (when-let [r ?reply-fn] (r {:braid/error "Bad url for avatar"})))))

(defmethod event-msg-handler :braid.server/set-password
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (string/blank? (?data :password))
    (when ?reply-fn (?reply-fn {:error "Password cannot be blank"}))
    (do
      (db/run-txns! (user/set-user-password-txn user-id (?data :password)))
      (when ?reply-fn (?reply-fn {:ok true})))))

(defmethod event-msg-handler :braid.server/set-preferences
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (db/run-txns!
    (mapcat
      (fn [[k v]] (user/user-set-preference-txn user-id k v))
      ?data))
  (when ?reply-fn (?reply-fn :braid/ok)))

(defmethod event-msg-handler :braid.server/hide-thread
  [{:as ev-msg :keys [?data user-id]}]
  (db/run-txns! (thread/user-hide-thread-txn user-id ?data))
  (chsk-send! user-id [:braid.client/hide-thread ?data]))

(defmethod event-msg-handler :braid.server/show-thread
  [{:as ev-msg :keys [?data user-id]}]
  (db/run-txns! (thread/user-show-thread-txn user-id ?data))
  (chsk-send! user-id [:braid.client/show-thread
                       (thread/thread-by-id ?data)]))

(defmethod event-msg-handler :braid.server/unsub-thread
  [{:as ev-msg :keys [?data user-id]}]
  (db/run-txns! (thread/user-unsubscribe-from-thread-txn user-id ?data))
  (chsk-send! user-id [:braid.client/hide-thread ?data]))

(defmethod event-msg-handler :braid.server/mark-thread-read
  [{:as ev-msg :keys [?data user-id]}]
  (thread/update-thread-last-open! ?data user-id))

(defmethod event-msg-handler :braid.server/create-tag
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (group/user-in-group? user-id (?data :group-id))
    (if (valid-tag-name? (?data :name))
      (let [[new-tag] (db/run-txns!
                      (tag/create-tag-txn (select-keys ?data [:id :name :group-id])))
            connected? (set (:any @connected-uids))
            users (group/group-users (:group-id new-tag))]
        (db/run-txns!
          (mapcat
            (fn [u] (tag/user-subscribe-to-tag-txn (u :id) (new-tag :id)))
            users))
        (doseq [u users]
          (when (connected? (u :id))
            (chsk-send! (u :id) [:braid.client/create-tag new-tag])))
        (when ?reply-fn
          (?reply-fn {:ok true})))
      (do (timbre/warnf "User %s attempted to create a tag %s with an invalid name"
                        user-id (?data :name))
          (when ?reply-fn
            (?reply-fn {:error "invalid tag name"}))))
    ; TODO: indicate permissions error to user?
    (timbre/warnf "User %s attempted to create a tag %s in a disallowed group"
                  user-id (?data :name) (?data :group-id))))

(defmethod event-msg-handler :braid.server/set-tag-description
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [{:keys [tag-id description]} ?data]
    (when (and tag-id description)
      (let [group-id (tag/tag-group-id tag-id)]
        (when (group/user-is-group-admin? user-id group-id)
          (db/run-txns! (tag/tag-set-description-txn tag-id description))
          (broadcast-group-change
            group-id [:braid.client/tag-descrption-change [tag-id description]]))))))

(defmethod event-msg-handler :braid.server/retract-tag
  [{:as ev-msg :keys [?data user-id]}]
  (let [tag-id (have util/uuid? ?data)
        group-id (tag/tag-group-id tag-id)]
    (when (group/user-is-group-admin? user-id group-id)
      (db/run-txns! (tag/retract-tag-txn tag-id))
      (broadcast-group-change group-id [:braid.client/retract-tag tag-id]))))

(defmethod event-msg-handler :braid.server/search
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  ; this can take a while, so move it to a future
  (future
    (let [user-tags (tag/tag-ids-for-user user-id)
          filter-tags (fn [t] (update-in t [:tag-ids] (partial into #{} (filter user-tags))))
          thread-ids (search/search-threads-as user-id ?data)
          threads (map (comp filter-tags thread/thread-by-id)
                       (take 25 thread-ids))]
      (when ?reply-fn
        (?reply-fn {:threads threads :thread-ids thread-ids})))))

(defmethod event-msg-handler :braid.server/load-recent-threads
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (when ?reply-fn
    (?reply-fn {:braid/ok (thread/recent-threads
                            {:group-id ?data
                             :user-id user-id})})))

(defmethod event-msg-handler :braid.server/load-threads
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [user-tags (tag/tag-ids-for-user user-id)
        filter-tags (fn [t] (update-in t [:tag-ids] (partial into #{} (filter user-tags))))
        thread-ids (filter (partial thread/user-can-see-thread? user-id) ?data)
        threads (map filter-tags (thread/threads-by-id thread-ids))]
    (when ?reply-fn
      (?reply-fn {:threads threads}))))

(defmethod event-msg-handler :braid.server/invite-to-group
  [{:as ev-msg :keys [?data user-id]}]
  (if (group/user-in-group? user-id (?data :group-id))
    (let [data (assoc ?data :inviter-id user-id)
          [invitation] (db/run-txns! (invitation/create-invitation-txn data))]
      (if-let [invited-user (user/user-with-email (invitation :invitee-email))]
        (chsk-send! (invited-user :id) [:braid.client/invitation-received invitation])
        (invites/send-invite invitation)))
    ; TODO: indicate permissions error to user?
    (timbre/warnf
      "User %s attempted to invite %s to a group %s they don't have access to"
      user-id (?data :invitee-email) (?data :group-id))))

(defmethod event-msg-handler :braid.server/generate-invite-link
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (group/user-in-group? user-id (?data :group-id))
    (let [{:keys [group-id expires]} ?data]
      (?reply-fn {:link (invites/make-open-invite-link group-id expires)}))
    (do (timbre/warnf
          "User %s attempted to invite %s to a group %s they don't have access to"
          user-id (?data :invitee-email) (?data :group-id))
        (?reply-fn {:braid/error :not-allowed}))))

(defmethod event-msg-handler :braid.server/invitation-accept
  [{:as ev-msg :keys [?data user-id]}]
  (if-let [invite (invitation/invite-by-id (?data :id))]
    (do
      (events/user-join-group! user-id (invite :group-id))
      (db/run-txns! (invitation/retract-invitation-txn (invite :id)))
      (chsk-send! user-id [:braid.client/joined-group
                           {:group (group/group-by-id (invite :group-id))
                            :tags (group/group-tags (invite :group-id))}])
      (chsk-send! user-id [:braid.client/update-users (user/users-for-user user-id)]))
    (timbre/warnf "User %s attempted to accept nonexistant invitaiton %s"
                  user-id (?data :id))))

(defmethod event-msg-handler :braid.server/invitation-decline
  [{:as ev-msg :keys [?data user-id]}]
  (if-let [invite (invitation/invite-by-id (?data :id))]
    (db/run-txns! (invitation/retract-invitation-txn (invite :id)))
    (timbre/warnf "User %s attempted to decline nonexistant invitaiton %s"
                  user-id (?data :id))))

(defmethod event-msg-handler :braid.server/make-user-admin
  [{:as ev-msg :keys [?data user-id]}]
  (let [{new-admin-id :user-id group-id :group-id} ?data]
    (when (and new-admin-id group-id
            (group/user-is-group-admin? user-id group-id))
      (db/run-txns! (group/user-make-group-admin-txn new-admin-id group-id))
      (broadcast-group-change group-id
                              [:braid.client/new-admin [group-id new-admin-id]]))))

(defmethod event-msg-handler :braid.server/remove-from-group
  [{:as ev-msg :keys [?data user-id]}]
  (let [{group-id :group-id to-remove-id :user-id} ?data]
    (when (and group-id to-remove-id
            (or (= to-remove-id user-id)
                (group/user-is-group-admin? user-id group-id)))
      (db/run-txns! (group/user-leave-group-txn to-remove-id group-id))
      (broadcast-group-change group-id [:braid.client/user-left
                                        [group-id to-remove-id]])
      (chsk-send!
        to-remove-id
        [:braid.client/left-group [group-id (:name (group/group-by-id group-id))]]))))

(defmethod event-msg-handler :braid.server/set-group-intro
  [{:as ev-msg :keys [?data user-id]}]
  (let [{:keys [group-id intro]} ?data]
    (when (and group-id (group/user-is-group-admin? user-id group-id))
      (db/run-txns! (group/group-set-txn group-id :intro intro))
      (broadcast-group-change group-id [:braid.client/new-intro [group-id intro]]))))

(defmethod event-msg-handler :braid.server/set-group-avatar
  [{:as ev-msg :keys [?data user-id]}]
  (let [{:keys [group-id avatar]} ?data]
    (when (and group-id (group/user-is-group-admin? user-id group-id))
      (db/run-txns! (group/group-set-txn group-id :avatar avatar))
      (broadcast-group-change group-id [:braid.client/group-new-avatar [group-id avatar]]))))

(defmethod event-msg-handler :braid.server/set-group-publicity
  [{:as ev-msg :keys [?data user-id]}]
  (let [[group-id publicity] ?data]
    (when (and group-id (group/user-is-group-admin? user-id group-id))
      (db/run-txns! (group/group-set-txn group-id :public? publicity))
      (broadcast-group-change group-id [:braid.client/publicity-changed [group-id publicity]]))))

(defmethod event-msg-handler :braid.server/create-bot
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [bot ?data
        reply-fn (or ?reply-fn (constantly nil))]
    (when (and (bot :group-id) (group/user-is-group-admin? user-id (bot :group-id)))
      (cond
        (not (re-matches util/bot-name-re (bot :name)))
        (do (timbre/warnf "User %s tried to create bot with invalid name %s"
                          user-id (bot :name))
            (reply-fn {:braid/error "Bad bot name"}))

        (not (valid-url? (bot :webhook-url)))
        (do (timbre/warnf "User %s tried to create bot with invalid webhook url %s"
                          user-id (bot :webhook-url))
            (reply-fn {:braid/error "Invalid webhook url for bot"}))

        (and (not (string/blank? (bot :event-webhook-url)))
             (not (valid-url? (bot :event-webhook-url))))
        (do (timbre/warnf "User %s tried to create bot with invalid event webhook url %s"
                          user-id (bot :event-webhook-url))
            (reply-fn {:braid/error "Invalid event webhook url for bot"}))

        (not (string? (bot :avatar)))
        (do (timbre/warnf "User %s tried to create bot without an avatar"
                          user-id (bot :webhook-url))
            (reply-fn {:braid/error "Bot needs an avatar image"}))

        :else
        (let [[created] (db/run-txns! (bot/create-bot-txn bot))]
          (reply-fn {:braid/ok created})
          (broadcast-group-change
            (bot :group-id)
            [:braid.client/new-bot [(bot :group-id) (bot->display created)]]))))))

(defmethod event-msg-handler :braid.server/edit-bot
  [{:as ev-msg bot :?data reply-fn :?reply-fn user-id :user-id}]
  (let [orig-bot (bot/bot-by-id (bot :id))]
    (when (group/user-is-group-admin? user-id (orig-bot :group-id))
      (cond
        (not (re-matches util/bot-name-re (bot :name)))
        (do (timbre/warnf "User %s tried to change bot to have an invalid name %s"
                          user-id (bot :name))
            (reply-fn {:braid/error "Bad bot name"}))

        (not (valid-url? (bot :webhook-url)))
        (do (timbre/warnf "User %s tried to change bot to have invalid webhook url %s"
                          user-id (bot :webhook-url))
            (reply-fn {:braid/error "Invalid webhook url for bot"}))

        (and (not (string/blank? (bot :event-webhook-url)))
             (not (valid-url? (bot :event-webhook-url))))
        (do (timbre/warnf "User %s tried to change bot to have invalid event webhook url %s"
                          user-id (bot :event-webhook-url))
            (reply-fn {:braid/error "Invalid event webhook url for bot"}))

        (not (string? (bot :avatar)))
        (do (timbre/warnf "User %s tried to change bot to not have an avatar"
                          user-id (bot :webhook-url))
            (reply-fn {:braid/error "Bot needs an avatar image"}))

        :else
        (let [updating (select-keys bot [:name :avatar :webhook-url
                                         :notify-all-messages?
                                         :event-webhook-url])
              updating (if (string/blank? (bot :event-webhook-url))
                         (dissoc updating :event-webhook-url)
                         updating)
              updating (reduce
                         (fn [m [k v]]
                           (if (= v (get orig-bot k))
                             m
                             (assoc m (keyword "bot" (name k)) v)))
                         {:db/id [:bot/id (bot :id)]}
                         updating)
              [updated] (db/run-txns! (bot/update-bot-txn updating))]
          (timbre/debugf "Updated bot: %s" updated)
          (reply-fn {:braid/ok updated})
          (broadcast-group-change
            (orig-bot :group-id)
            [:braid.client/edit-bot [(orig-bot :group-id) (bot->display updated)]]))))))

(defmethod event-msg-handler :braid.server/retract-bot
  [{:as ev-msg bot-id :?data reply-fn :?reply-fn user-id :user-id}]
  (let [bot (bot/bot-by-id bot-id)]
    (when (group/user-is-group-admin? user-id (bot :group-id))
      (db/run-txns! (bot/retract-bot-txn bot-id))
      (reply-fn {:braid/ok true})
      (broadcast-group-change
        (bot :group-id)
        [:braid.client/retract-bot [(bot :group-id) bot-id]]))))

(defmethod event-msg-handler :braid.server/get-bot-info
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [bot (bot/bot-by-id ?data)]
    (when (and bot (group/user-is-group-admin? user-id (bot :group-id)) ?reply-fn)
      (?reply-fn {:braid/ok bot}))))

(defmethod event-msg-handler :braid.server/create-upload
  [{:as ev-msg :keys [?data user-id]}]
  (let [upload (assoc ?data
                 :uploaded-at (java.util.Date.)
                 :uploader-id user-id)]
    (when (and (upload-valid? upload)
               (group/user-in-group?
                 user-id
                 (thread/thread-group-id (upload :thread-id))))
      (db/run-txns! (upload/create-upload-txn upload)))))

(defmethod event-msg-handler :braid.server/uploads-in-group
  [{:as ev-msg :keys [?data user-id ?reply-fn]}]
  (when ?reply-fn
    (if (group/user-in-group? user-id ?data)
      (?reply-fn {:braid/ok (upload/uploads-in-group ?data)})
      (?reply-fn {:braid/error "Not allowed"}))))

(defmethod event-msg-handler :braid.server/start
  [{:as ev-msg :keys [user-id]}]
  (let [connected (set (:any @connected-uids))]
    (chsk-send!
      user-id
      [:braid.client/init-data
       {:user-id user-id
        :version-checksum (if (= "prod" (env :environment))
                            (digest/from-file "public/js/prod/desktop.js")
                            (digest/from-file "public/js/dev/desktop.js"))
        :user-groups (group/user-groups user-id)
        :user-threads (thread/open-threads-for-user user-id)
        :user-subscribed-tag-ids (tag/subscribed-tag-ids-for-user user-id)
        :user-preferences (user/user-get-preferences user-id)
        :users (into ()
                     (map #(assoc % :status
                             (if (connected (% :id)) :online :offline)))
                     (user/users-for-user user-id))
        :invitations (invitation/invites-for-user user-id)
        :tags (tag/tags-for-user user-id)
        :quest-records (quests/get-active-quests-for-user-id user-id)}])))
