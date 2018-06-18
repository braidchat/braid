(ns braid.core.client.state.remote-handlers
  (:require
   [braid.core.client.router :as router]
   [braid.core.client.sync :as sync]
   [braid.core.hooks :as hooks]
   [re-frame.core :refer [dispatch]]
   [taoensso.timbre :as timbre :refer-macros [errorf]]))

(defonce incoming-socket-message-handlers
  (hooks/register! (atom {}) {keyword? fn?}))

(defmethod sync/event-handler :default
  [[id data]]
  (if-let [handler (@incoming-socket-message-handlers id)]
    (handler id data)
    (errorf "No socket message handler for id: %s" id)))

(defmethod sync/event-handler :braid.client/thread
  [[_ data]]
  (dispatch [:add-open-thread data])
  (dispatch [:maybe-increment-unread]))

(defmethod sync/event-handler :braid.client/init-data
  [[_ data]]
  (dispatch [:set-init-data data])
  (router/dispatch-current-path!)
  (dispatch [:notify-if-client-out-of-date (data :version-checksum)]))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (sync/chsk-send! [:braid.server/start nil]))

(defmethod sync/event-handler :braid.client/create-tag
  [[_ data]]
  (dispatch [:create-tag {:tag data
                          :local-only? true}]))

(defmethod sync/event-handler :braid.client/joined-group
  [[_ data]]
  (dispatch [:join-group data]))

(defmethod sync/event-handler :braid.client/update-users
  [[_ data]]
  (dispatch [:add-users data]))

(defmethod sync/event-handler :braid.client/invitation-received
  [[_ invite]]
  (dispatch [:add-invite invite]))

(defmethod sync/event-handler :braid.client/name-change
  [[_ {:keys [user-id nickname group-ids] :as data}]]
  (dispatch [:update-user-nickname data]))

(defmethod sync/event-handler :braid.client/user-new-avatar
  [[_ {:keys [user-id group-id avatar-url] :as data}]]
  (dispatch [:update-user-avatar data]))

(defmethod sync/event-handler :braid.client/left-group
  [[_ [group-id group-name]]]
  (dispatch [:leave-group {:group-id group-id
                           :group-name group-name}]))

(defmethod sync/event-handler :braid.client/user-connected
  [[_ [group-id user-id]]]
  (dispatch [:update-user-status [group-id user-id :online]]))

(defmethod sync/event-handler :braid.client/user-disconnected
  [[_ [group-id user-id]]]
  (dispatch [:update-user-status [group-id user-id :offline]]))

(defmethod sync/event-handler :braid.client/new-user
  [[_ [user new-group]]]
  (dispatch [:add-user new-group (assoc user :status :online)]))

(defmethod sync/event-handler :braid.client/user-left
  [[_ [group-id user-id]]]
  (dispatch [:remove-user-from-group [user-id group-id]]))

(defmethod sync/event-handler :braid.client/new-admin
  [[_ [group-id new-admin-id]]]
  (dispatch [:make-admin {:group-id group-id
                          :user-id new-admin-id
                          :local-only? true}]))

(defmethod sync/event-handler :braid.client/tag-descrption-change
  [[_ [tag-id new-description]]]
  (dispatch [:set-tag-description {:tag-id tag-id
                                   :description new-description
                                   :local-only? true}]))

(defmethod sync/event-handler :braid.client/retract-tag
  [[ _ tag-id]]
  (dispatch [:remove-tag {:tag-id tag-id :local-only? true}]))

(defmethod sync/event-handler :braid.client/new-intro
  [[_ [group-id intro]]]
  (dispatch [:set-group-intro {:group-id group-id
                               :intro intro
                               :local-only? true}]))

(defmethod sync/event-handler :braid.client/group-new-avatar
  [[_ [group-id avatar]]]
  (dispatch [:set-group-avatar {:group-id group-id
                                :avatar avatar
                                :local-only? true}]))

(defmethod sync/event-handler :braid.client/publicity-changed
  [[_ [group-id publicity]]]
  (dispatch [:set-group-publicity [group-id publicity]]))

(defmethod sync/event-handler :braid.client/new-bot
  [[_ [group-id bot]]]
  (dispatch [:add-group-bot [group-id bot]]))

(defmethod sync/event-handler :braid.client/retract-bot
  [[_ [group-id bot-id]]]
  (dispatch [:remove-group-bot [group-id bot-id]]))

(defmethod sync/event-handler :braid.client/edit-bot
  [[_ [group-id bot]]]
  (dispatch [:update-group-bot [group-id bot]]))

(defmethod sync/event-handler :braid.client/notify-message
  [[_ message]]
  (dispatch [:core/show-message-notification message]))

(defmethod sync/event-handler :braid.client/hide-thread
  [[_ thread-id]]
  (dispatch [:hide-thread {:thread-id thread-id :local-only? true}]))

(defmethod sync/event-handler :braid.client/show-thread
  [[_ thread]]
  (dispatch [:add-open-thread thread]))

(defmethod sync/event-handler :braid.client/message-deleted
  [[_ {:keys [message-id thread-id] :as info}]]
  (dispatch [:core/retract-message (assoc info :remote? false)]))
