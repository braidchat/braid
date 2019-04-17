(ns braid.core.server.sync
  (:require
    [clojure.set :refer [difference intersection]]
    [clojure.string :as string]
    [environ.core :refer [env]]
    [mount.core :refer [defstate]]
    [taoensso.timbre :as timbre :refer [debugf]]
    [taoensso.truss :refer [have]]
    [braid.core.hooks :as hooks]
    [braid.core.common.schema :as schema]
    [braid.core.common.util :as util :refer [valid-nickname? valid-tag-name?]]
    [braid.core.server.db :as db]
    [braid.core.server.db.group :as group]
    [braid.core.server.db.invitation :as invitation]
    [braid.core.server.db.message :as message]
    [braid.core.server.db.tag :as tag]
    [braid.core.server.db.thread :as thread]
    [braid.core.server.db.user :as user]
    [braid.core.server.digest :as digest]
    [braid.core.server.events :as events]
    [braid.core.server.invite :as invites]
    [braid.core.server.message-format :refer [parse-tags-and-mentions]]
    [braid.core.server.socket :refer [chsk-send! connected-uids]]
    [braid.core.server.sync-handler :refer [event-msg-handler]]
    [braid.core.server.sync-helpers :as helpers :refer [broadcast-group-change]]
    [braid.core.server.util :refer [valid-url?]]))

(defn user-can-delete-message?
  [user-id message-id]
  (or (= user-id (message/message-author message-id))
      (group/user-is-group-admin?
        user-id
        (message/message-group message-id))))

(defn broadcast-new-user-to-group
  [user-id group-id]
  (broadcast-group-change
    group-id
    [:braid.client/new-user [(user/user-by-id user-id) group-id]]))

;; Handlers

(defmethod event-msg-handler :chsk/ws-ping
  [ev-msg])
  ; Do nothing, just avoid unhandled event message


(defmethod event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [user-id]}]
  (doseq [{group-id :id} (group/user-groups user-id)]
    (broadcast-group-change group-id [:braid.client/user-connected [group-id user-id]])))

(defmethod event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [user-id]}]
  (doseq [{group-id :id} (group/user-groups user-id)]
    (broadcast-group-change group-id [:braid.client/user-disconnected [group-id user-id]])))

(defonce new-message-callbacks (hooks/register! (atom []) [fn?]))

(defmethod event-msg-handler :braid.server/new-message
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [new-message (-> ?data
                        (update :mentioned-tag-ids vec)
                        (update :mentioned-user-ids vec)
                        (update-in [:content] #(apply str (take 5000 %)))
                        (assoc :created-at (java.util.Date.)))]
    (if (util/valid? schema/NewMessage new-message)
      (when (helpers/user-can-message? user-id new-message)
        (db/run-txns! (message/create-message-txn new-message))
        (when-let [cb ?reply-fn]
          (cb :braid/ok))
        ;; [TODO] should we put these in a future?
        (doseq [callback @new-message-callbacks]
          (callback new-message))
        (helpers/broadcast-thread (new-message :thread-id) [])
        (helpers/notify-users new-message))
      (do
        (timbre/warnf "Malformed new message: %s" (pr-str new-message))
        (when-let [cb ?reply-fn]
          (cb :braid/error))))))

(defmethod event-msg-handler :braid.server/retract-message
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (let [message-id ?data]
    (when (user-can-delete-message? user-id message-id)
      (let [msg-group (message/message-group message-id)
            msg-thread (message/message-thread message-id)]
        (db/run-txns! (message/retract-message-txn message-id))
        (broadcast-group-change msg-group
                                [:braid.client/message-deleted
                                 {:message-id message-id
                                  :thread-id msg-thread}])))))

(defmethod event-msg-handler :braid.server/tag-thread
  [{:as ev-msg :keys [?data user-id]}]
  (let [{:keys [thread-id tag-id]} ?data]
    (let [group-id (tag/tag-group-id tag-id)]
      (when (group/user-in-group? user-id group-id)
        ;; [TODO] do we need to notify-users and notify-bots
        (db/run-txns! (thread/tag-thread-txn group-id thread-id tag-id))
        (helpers/broadcast-thread thread-id [])))))

(defmethod event-msg-handler :braid.server/mention-thread
  [{:as ev-msg :keys [?data] tagger-id :user-id}]
  (let [{:keys [thread-id user-id group-id]} ?data]
    (when (and (group/user-in-group? tagger-id group-id)
               (group/user-in-group? user-id group-id)
               (or (nil? (thread/thread-by-id thread-id))
                   (= group-id (:group-id (thread/thread-by-id thread-id)))))
      (db/run-txns! (thread/mention-thread-txn group-id thread-id user-id))
      (helpers/broadcast-thread thread-id []))))

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
          (doseq [group (group/user-groups user-id)]
            (helpers/broadcast-group-change
              (group :id)
              [:braid.client/name-change
               {:user-id user-id
                :group-id (group :id)
                :nickname (?data :nickname)}]))
          (when ?reply-fn (?reply-fn {:ok true})))
      (catch java.util.concurrent.ExecutionException _
        (when ?reply-fn (?reply-fn {:error "Nickname taken"}))))
    (when ?reply-fn (?reply-fn {:error "Invalid nickname"}))))

(defmethod event-msg-handler :braid.server/set-user-avatar
  [{:as ev-msg :keys [?data ?reply-fn user-id]}]
  (if (valid-url? ?data)
    (do (db/run-txns! (user/set-user-avatar-txn user-id ?data))
        (doseq [{group-id :id} (group/user-groups user-id)]
          (broadcast-group-change group-id [:braid.client/user-new-avatar
                                            {:user-id user-id
                                             :group-id group-id
                                             :avatar-url ?data}]))
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
        (broadcast-group-change (:group-id new-tag) [:braid.client/create-tag new-tag])
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
  (let [tag-id (have uuid? ?data)
        group-id (tag/tag-group-id tag-id)]
    (when (group/user-is-group-admin? user-id group-id)
      (db/run-txns! (tag/retract-tag-txn tag-id))
      (broadcast-group-change group-id [:braid.client/retract-tag tag-id]))))

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
        threads (into ()
                      (comp (map filter-tags)
                            (map #(thread/thread-add-last-open-at % user-id)))
                      (thread/threads-by-id thread-ids))]
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
      (chsk-send! user-id [:braid.client/update-users
                           [(invite :group-id) (user/users-for-user user-id)]]))
    (timbre/warnf "User %s attempted to accept nonexistant invitaiton %s"
                  user-id (?data :id))))

(defmethod event-msg-handler :braid.server/invitation-decline
  [{:as ev-msg :keys [?data user-id]}]
  (if-let [invite (invitation/invite-by-id (?data :id))]
    (db/run-txns! (invitation/retract-invitation-txn (invite :id)))
    (timbre/warnf "User %s attempted to decline nonexistant invitaiton %s"
                  user-id (?data :id))))

(defmethod event-msg-handler :braid.server/join-public-group
  [{:as ev-msg :keys [?data user-id]}]
  (let [group (group/group-by-id ?data)]
    (if (:public? group)
      (do
        (events/user-join-group! user-id (group :id))
        (chsk-send! user-id [:braid.client/joined-group
                             {:group group
                              :tags (group/group-tags (group :id))}])
        (chsk-send! user-id [:braid.client/update-users
                             [(group :id) (user/users-for-user user-id)]]))
      (timbre/warnf "User %s attempted to join nonexistant or private group %s"
                    user-id ?data))))

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

(defmethod event-msg-handler :braid.server.anon/load-group
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn user-id]}]
  (when-let [group (group/group-by-id ?data)]
    (if (:public? group)
      (do (?reply-fn {:tags (group/group-tags ?data)
                      :group (assoc group :readonly true)
                      :threads (thread/public-threads ?data)})
          (helpers/add-anonymous-reader ?data user-id))
      (?reply-fn :braid/error))))

(defonce initial-user-data (hooks/register! (atom [])))

(defmethod event-msg-handler :braid.server/start
  [{:as ev-msg :keys [user-id]}]
  (let [connected (set (:any @connected-uids))
        dynamic-data (->> @initial-user-data
                         (into {} (map (fn [f] (f user-id)))))
        user-status (fn [user] (if (connected (user :id)) :online :offline))
        update-user-statuses (fn [users]
                               (reduce-kv
                                 (fn [m id u]
                                   (assoc m id (assoc u :status (user-status u))))
                                 {} users))]
    (chsk-send!
      user-id
      [:braid.client/init-data
       (merge
         {:user-id user-id
          :version-checksum (if (= "prod" (env :environment))
                              (digest/from-file "public/js/prod/desktop.js")
                              (digest/from-file "public/js/dev/desktop.js"))
          :user-groups
          (->> (group/user-groups user-id)
              (map (fn [group] (update group :users update-user-statuses))))
          :user-threads (thread/open-threads-for-user user-id)
          :user-subscribed-tag-ids (tag/subscribed-tag-ids-for-user user-id)
          :user-preferences (user/user-get-preferences user-id)
          :invitations (invitation/invites-for-user user-id)
          :tags (tag/tags-for-user user-id)}
         dynamic-data)])))
