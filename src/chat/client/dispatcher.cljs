(ns chat.client.dispatcher
  (:require [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]
            [cljs-utils.core :refer [edn-xhr]]))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (let [message (schema/make-message {:user-id (get-in @store/app-state [:session :user-id])
                                      :content (data :content)
                                      :thread-id (data :thread-id)})]
    (store/add-message! message)
    (sync/chsk-send! [:chat/new-message message])))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:chat/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :create-tag [_ tag-name]
  (let [tag (schema/make-tag {:name tag-name})]
    (store/add-tag! tag)
    (sync/chsk-send! [:chat/create-tag tag])))

(defmethod dispatch! :unsubscribe-from-tag [_ tag-id]
  (sync/chsk-send! [:user/unsubscribe-from-tag tag-id])
  (store/unsubscribe-from-tag! tag-id))

(defmethod dispatch! :subscribe-to-tag [_ tag-id]
  (sync/chsk-send! [:user/subscribe-to-tag tag-id])
  (store/subscribe-to-tag! tag-id))

(defmethod dispatch! :tag-thread [_ attr]
  (when-let [tag-id (store/tag-id-for-name (attr :tag-name))]
    (sync/chsk-send! [:thread/add-tag {:thread-id (attr :thread-id)
                                       :tag-id tag-id}])
    (store/add-tag-to-thread! tag-id (attr :thread-id))))

(defmethod dispatch! :auth [_ data]
  (edn-xhr {:url "/auth"
            :method :post
            :data {:email (data :email)
                   :password (data :password)
                   :csrf-token (:csrf-token @sync/chsk-state)}
            :on-error (fn [e]
                        ; TODO
                        )
            :on-complete (fn [data]
                           (sync/reconnect!))}))

(defmethod dispatch! :logout [_ _]
  (edn-xhr {:url "/logout"
            :method :post
            :data {:csrf-token (:csrf-token @sync/chsk-state)}
            :on-complete (fn [data]
                           (store/clear-session!))}))

; Websocket Events

(defmethod sync/event-handler :chat/new-message
  [[_ data]]
  (store/add-message! data))

(defmethod sync/event-handler :session/init-data
  [[_ data]]
  (store/set-session! {:user-id (data :user-id)})
  (store/add-users! (data :users))
  (store/add-tags! (data :tags))
  (store/set-user-subscribed-tag-ids! (data :user-subscribed-tag-ids))
  (store/set-threads! (data :user-threads)))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (sync/chsk-send! [:session/start nil]))

(defmethod sync/event-handler :thread/add-tag
  [[_ data]]
  (store/add-tag-to-thread! (data :tag-id) (data :thread-id)))

(defmethod sync/event-handler :chat/create-tag
  [[_ data]]
  (store/add-tag! data))
