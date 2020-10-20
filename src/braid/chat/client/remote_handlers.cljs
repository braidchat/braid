(ns braid.chat.client.remote-handlers
  (:require
   [braid.base.api :as base]
   [re-frame.core :refer [dispatch]]))

(defn init! []
  (base/register-incoming-socket-message-handlers!
    {:braid.client/thread
     (fn [_ data]
       (dispatch [:add-open-thread data])
       (dispatch [:maybe-increment-unread]))

     :braid.client/create-tag
     (fn [_ data]
       (dispatch [:create-tag {:tag data
                               :local-only? true}]))

     :braid.client/joined-group
     (fn [_ data]
       (dispatch [:join-group data]))

     :braid.client/update-users
     (fn [_ data]
       (dispatch [:add-users data]))

     :braid.client/invitation-received
     (fn [_ invite]
       (dispatch [:add-invite invite]))

     :braid.client/name-change
     (fn [_ {:keys [user-id nickname group-ids] :as data}]
       (dispatch [:update-user-nickname data]))

     :braid.client/user-new-avatar
     (fn [_ {:keys [user-id group-id avatar-url] :as data}]
       (dispatch [:update-user-avatar data]))

     :braid.client/left-group
     (fn [_ [group-id group-name]]
       (dispatch [:leave-group {:group-id group-id
                                :group-name group-name}]))

     :braid.client/user-connected
     (fn [_ [group-id user-id]]
       (dispatch [:update-user-status [group-id user-id :online]]))

     :braid.client/user-disconnected
     (fn [_ [group-id user-id]]
       (dispatch [:update-user-status [group-id user-id :offline]]))

     :braid.client/new-user
     (fn [_ [user new-group]]
       (dispatch [:add-user new-group (assoc user :status :online)]))

     :braid.client/user-left
     (fn [_ [group-id user-id]]
       (dispatch [:remove-user-from-group [user-id group-id]]))

     :braid.client/new-admin
     (fn [_ [group-id new-admin-id]]
       (dispatch [:make-admin {:group-id group-id
                               :user-id new-admin-id
                               :local-only? true}]))

     :braid.client/tag-descrption-change
     (fn [_ [tag-id new-description]]
       (dispatch [:set-tag-description {:tag-id tag-id
                                        :description new-description
                                        :local-only? true}]))

     :braid.client/retract-tag
     (fn [ _ tag-id]
       (dispatch [:remove-tag {:tag-id tag-id :local-only? true}]))

     :braid.client/new-intro
     (fn [_ [group-id intro]]
       (dispatch [:set-group-intro {:group-id group-id
                                    :intro intro
                                    :local-only? true}]))

     :braid.client/group-new-avatar
     (fn [_ [group-id avatar]]
       (dispatch [:set-group-avatar {:group-id group-id
                                     :avatar avatar
                                     :local-only? true}]))

     :braid.client/publicity-changed
     (fn [_ [group-id publicity]]
       (dispatch [:set-group-publicity [group-id publicity]]))

     :braid.client/notify-message
     (fn [_ message]
       (dispatch [:core/show-message-notification message]))

     :braid.client/hide-thread
     (fn [_ thread-id]
       (dispatch [:hide-thread {:thread-id thread-id :local-only? true}]))

     :braid.client/show-thread
     (fn [_ thread]
       (dispatch [:add-open-thread thread]))

     :braid.client/message-deleted
     (fn [_ {:keys [message-id thread-id] :as info}]
       (dispatch [:core/retract-message (assoc info :remote? false)]))}))
