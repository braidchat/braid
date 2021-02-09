(ns braid.chat.socket-message-handlers
  (:require
    [clojure.string :as string]
    [taoensso.timbre :as timbre]
    [taoensso.truss :refer [have]]
    [braid.core.hooks :as hooks]
    [braid.core.common.schema :as schema]
    [braid.core.common.util :as util :refer [valid-nickname? valid-tag-name?]]
    [braid.core.server.db :as db]
    [braid.chat.db.group :as group]
    [braid.chat.db.invitation :as invitation]
    [braid.chat.db.message :as message]
    [braid.chat.db.tag :as tag]
    [braid.chat.db.thread :as thread]
    [braid.chat.db.user :as user]
    [braid.chat.events :as events]
    [braid.core.server.invite :as invites]
    [braid.base.server.socket :refer [chsk-send!]]
    [braid.core.server.sync-handler :as sync-handler]
    [braid.core.server.sync-helpers :as helpers :refer [broadcast-group-change]]
    [braid.lib.url :refer [valid-url?]]))

(defn user-can-delete-message?
  [user-id message-id]
  (or (= user-id (message/message-author message-id))
      (group/user-is-group-admin?
        user-id
        (message/message-group message-id))))

(defonce new-message-callbacks (hooks/register! (atom []) [fn?]))

(def socket-message-handlers
  {:chsk/uidport-open
   (fn [{:keys [user-id]}]
     (doseq [{group-id :id} (group/user-groups user-id)]
       (broadcast-group-change group-id [:braid.client/user-connected [group-id user-id]]))
     ;; TODO rewrite to use cofx
     {})

   :chsk/uidport-close
   (fn [{:keys [user-id]}]
     (doseq [{group-id :id} (group/user-groups user-id)]
       (broadcast-group-change group-id [:braid.client/user-disconnected [group-id user-id]]))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/retract-message
   (fn [{:keys [?data user-id]}]
     (let [message-id ?data]
       (when (user-can-delete-message? user-id message-id)
         (let [msg-group (message/message-group message-id)
               msg-thread (message/message-thread message-id)]
           (db/run-txns! (message/retract-message-txn message-id))
           (broadcast-group-change msg-group
                                   [:braid.client/message-deleted
                                    {:message-id message-id
                                     :thread-id msg-thread}]))))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/tag-thread
   (fn [{:keys [?data user-id]}]
     (let [{:keys [thread-id tag-id]} ?data]
       (let [group-id (tag/tag-group-id tag-id)]
         (when (group/user-in-group? user-id group-id)
           ;; [TODO] do we need to notify-users and notify-bots
           (db/run-txns! (thread/tag-thread-txn group-id thread-id tag-id))
           (helpers/broadcast-thread thread-id []))))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/mention-thread
   (fn [{:keys [?data] tagger-id :user-id}]
     (let [{:keys [thread-id user-id group-id]} ?data]
       (when (and (group/user-in-group? tagger-id group-id)
               (group/user-in-group? user-id group-id)
               (or (nil? (thread/thread-by-id thread-id))
                   (= group-id (:group-id (thread/thread-by-id thread-id)))))
         (db/run-txns! (thread/mention-thread-txn group-id thread-id user-id))
         (helpers/broadcast-thread thread-id [])))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/subscribe-to-tag
   (fn [{:keys [?data user-id]}]
     {:db-run-txns! (tag/user-subscribe-to-tag-txn user-id ?data)})

   :braid.server/unsubscribe-from-tag
   (fn [{:keys [?data user-id]}]
     {:db-run-txns! (tag/user-unsubscribe-from-tag-txn user-id ?data)})

   :braid.server/set-nickname
   (fn [{:keys [?data ?reply-fn user-id]}]
     (if (valid-nickname? (?data :nickname))
       (try
         (db/run-txns! (user/set-nickname-txn user-id (?data :nickname)))
         (doseq [group (group/user-groups user-id)]
           (helpers/broadcast-group-change
             (group :id)
             [:braid.client/name-change
              {:user-id user-id
               :group-id (group :id)
               :nickname (?data :nickname)}]))
         (when ?reply-fn (?reply-fn {:ok true}))
         (catch java.util.concurrent.ExecutionException _
           (when ?reply-fn (?reply-fn {:error "Nickname taken"}))))
       (when ?reply-fn (?reply-fn {:error "Invalid nickname"})))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/set-user-avatar
   (fn [{:keys [?data ?reply-fn user-id]}]
     (if (valid-url? ?data)
       (do (db/run-txns! (user/set-user-avatar-txn user-id ?data))
           (doseq [{group-id :id} (group/user-groups user-id)]
             (broadcast-group-change group-id [:braid.client/user-new-avatar
                                               {:user-id user-id
                                                :group-id group-id
                                                :avatar-url ?data}]))
           (when-let [r ?reply-fn] (r {:braid/ok true})))
       (do (timbre/warnf "Couldn't set user avatar to %s" ?data)
           (when-let [r ?reply-fn] (r {:braid/error "Bad url for avatar"}))))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/set-password
   (fn [{:keys [?data ?reply-fn user-id]}]
     (if (string/blank? (?data :password))
       (when ?reply-fn (?reply-fn {:error "Password cannot be blank"}))
       (do
         (db/run-txns! (user/set-user-password-txn user-id (?data :password)))
         (when ?reply-fn (?reply-fn {:ok true}))))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/set-preferences
   (fn [{:keys [?data ?reply-fn user-id]}]
     (db/run-txns!
       (mapcat
         (fn [[k v]] (user/user-set-preference-txn user-id k v))
         ?data))
     (when ?reply-fn (?reply-fn :braid/ok))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/unsub-thread
   (fn [{:keys [?data user-id]}]
     {:db-run-txns! (thread/user-unsubscribe-from-thread-txn user-id ?data)
      :chsk-send! [user-id [:braid.client/hide-thread ?data]]})

   :braid.server/mark-thread-read
   (fn [{:keys [?data user-id]}]
     (thread/update-thread-last-open! ?data user-id)
     ;; TODO rewrite to use cofx
     {})

   :braid.server/set-tag-description
   (fn [{:keys [?data user-id]}]
     (let [{:keys [tag-id description]} ?data]
       (when (and tag-id description)
         (let [group-id (tag/tag-group-id tag-id)]
           (when (group/user-is-group-admin? user-id group-id)
             {:db-run-txns! (tag/tag-set-description-txn tag-id description)
              :group-broadcast! [group-id [:braid.client/tag-descrption-change [tag-id description]]]})))))

   :braid.server/load-threads
   (fn [{:keys [?data ?reply-fn user-id]}]
     (let [user-tags (tag/tag-ids-for-user user-id)
           filter-tags (fn [t] (update-in t [:tag-ids] (partial into #{} (filter user-tags))))
           thread-ids (filter (partial thread/user-can-see-thread? user-id) ?data)
           threads (into ()
                         (comp (map filter-tags)
                               (map #(thread/thread-add-last-open-at % user-id)))
                         (thread/threads-by-id thread-ids))]
       (when ?reply-fn
         (?reply-fn {:threads threads})))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/retract-tag
   (fn [{:keys [?data user-id]}]
     (let [tag-id (have uuid? ?data)
           group-id (tag/tag-group-id tag-id)]
       (when (group/user-is-group-admin? user-id group-id)
         {:db-run-txns! (tag/retract-tag-txn tag-id)
          :group-broadcast! [group-id [:braid.client/retract-tag tag-id]]})))

   :braid.server/invite-to-group
   (fn [{:keys [?data user-id]}]
     (if (group/user-in-group? user-id (?data :group-id))
       (let [data (assoc ?data :inviter-id user-id)
             [invitation] (db/run-txns! (invitation/create-invitation-txn data))]
         (if-let [invited-user (user/user-with-email (invitation :invitee-email))]
           (chsk-send! (invited-user :id) [:braid.client/invitation-received invitation])
           (invites/send-invite invitation)))
       ; TODO: indicate permissions error to user?
       (timbre/warnf
         "User %s attempted to invite %s to a group %s they don't have access to"
         user-id (?data :invitee-email) (?data :group-id)))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/generate-invite-link
   (fn [{:keys [?data ?reply-fn user-id]}]
     (if (group/user-in-group? user-id (?data :group-id))
       (let [{:keys [group-id expires]} ?data]
         (?reply-fn {:link (invites/make-open-invite-link group-id expires)}))
       (do (timbre/warnf
             "User %s attempted to invite %s to a group %s they don't have access to"
             user-id (?data :invitee-email) (?data :group-id))
           (?reply-fn {:braid/error :not-allowed})))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/invitation-accept
   (fn [{:keys [?data user-id]}]
     (if-let [invite (invitation/invite-by-id (?data :id))]
       (let [joined-at (group/group-users-joined-at (invite :group-id))]
         (events/user-join-group! user-id (invite :group-id))
         (db/run-txns! (invitation/retract-invitation-txn (invite :id)))
         (chsk-send! user-id [:braid.client/joined-group
                              {:group (group/group-by-id (invite :group-id))
                               :tags (group/group-tags (invite :group-id))}])
         (chsk-send! user-id
                     [:braid.client/update-users
                      [(invite :group-id)
                       (->> (user/users-for-user user-id)
                            (map (fn [user]
                                   (assoc user :joined-at (joined-at (user :id))))))]]))
       (timbre/warnf "User %s attempted to accept nonexistant invitaiton %s"
                     user-id (?data :id)))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/invitation-decline
   (fn [{:keys [?data user-id]}]
     (if-let [invite (invitation/invite-by-id (?data :id))]
       (db/run-txns! (invitation/retract-invitation-txn (invite :id)))
       (timbre/warnf "User %s attempted to decline nonexistant invitaiton %s"
                     user-id (?data :id)))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/join-public-group
   (fn [{:keys [?data user-id]}]
     (let [group (group/group-by-id ?data)]
       (if (:public? group)
         (let [joined-at (group/group-users-joined-at (:id group))]
           (events/user-join-group! user-id (group :id))
           (chsk-send! user-id [:braid.client/joined-group
                                {:group group
                                 :tags (group/group-tags (group :id))}])
           (chsk-send! user-id
                       [:braid.client/update-users
                        [(group :id)
                         (->> (user/users-for-user user-id)
                              (map (fn [user]
                                     (assoc user :joined-at (joined-at (user :id))))))]]))
         (timbre/warnf "User %s attempted to join nonexistant or private group %s"
                       user-id ?data)))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/make-user-admin
   (fn [{:keys [?data user-id]}]
     (let [{new-admin-id :user-id group-id :group-id} ?data]
       (when (and new-admin-id group-id
               (group/user-is-group-admin? user-id group-id))
         {:db-run-txns! (group/user-make-group-admin-txn new-admin-id group-id)
          :group-broadcast! [group-id [:braid.client/new-admin [group-id new-admin-id]]]})))

   :braid.server/remove-from-group
   (fn [{:keys [?data user-id]}]
     (let [{group-id :group-id to-remove-id :user-id} ?data]
       (when (and group-id to-remove-id
               (or (= to-remove-id user-id)
                   (group/user-is-group-admin? user-id group-id)))
         (db/run-txns! (group/user-leave-group-txn to-remove-id group-id))
         (broadcast-group-change group-id [:braid.client/user-left
                                           [group-id to-remove-id]])
         (chsk-send!
           to-remove-id
           [:braid.client/left-group [group-id (:name (group/group-by-id group-id))]])))
     ;; TODO rewrite to use cofx
     {})

   :braid.server/set-group-intro
   (fn [{:keys [?data user-id]}]
     (let [{:keys [group-id intro]} ?data]
       (when (and group-id (group/user-is-group-admin? user-id group-id))
         {:db-run-txns! (group/group-set-txn group-id :intro intro)
          :group-broadcast! [group-id [:braid.client/new-intro [group-id intro]]]})))

   :braid.server/set-group-avatar
   (fn [{:keys [?data user-id]}]
     (let [{:keys [group-id avatar]} ?data]
       (when (and group-id (group/user-is-group-admin? user-id group-id))
         {:db-run-txns! (group/group-set-txn group-id :avatar avatar)
          :group-broadcast! [group-id [:braid.client/group-new-avatar [group-id avatar]]]})))

   :braid.server/set-group-publicity
   (fn [{:keys [?data user-id]}]
     (let [[group-id publicity] ?data]
       (when (and group-id (group/user-is-group-admin? user-id group-id))
         {:db-run-txns! (group/group-set-txn group-id :public? publicity)
          :group-broadcast! [group-id [:braid.client/publicity-changed [group-id publicity]]]})))

   ;; need to duplicate the anonymous handler for logged in users; this
   ;; is used when clicking on a public group from group explore while
   ;; logged in. Difference is we want to add the "anonymous" reader as
   ;; the logged-in user
   :braid.server.anon/load-group
   (fn [{:keys [?data ?reply-fn user-id]}]
     (when-let [group (group/group-by-id ?data)]
       (when (:public? group)
         (?reply-fn (reduce (fn [m f] (f (group :id) m))
                            {:tags (group/group-tags ?data)
                             :group (assoc group :readonly true)
                             :threads (thread/public-threads ?data)}
                            @sync-handler/anonymous-load-group))
         (helpers/add-anonymous-reader ?data user-id)))
     ;; TODO rewrite to use cofx
     {})})
