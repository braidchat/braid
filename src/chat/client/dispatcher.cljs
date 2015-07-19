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
    (store/show-thread! (message :thread-id))
    (sync/chsk-send! [:chat/new-message message])))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:chat/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

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
  (store/add-message! data)
  (store/show-thread! (data :thread-id)))

(defmethod sync/event-handler :session/init-data
  [[_ data]]
  (store/set-session! {:user-id (data :user-id)})
  (store/add-users! (data :users))
  (store/add-messages! (data :messages))
  (store/set-open-thread-ids! (data :open-thread-ids)))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (sync/chsk-send! [:session/start nil]))
