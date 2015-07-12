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
  (store/transact! [:users (get-in @store/app-state [:session :user-id]) :hidden-thread-ids] #(conj % (data :thread-id))))

(defn start-session! [user-id]
  (store/set-session! {:user-id user-id})
  (sync/chsk-send! [:session/start nil]))

(defmethod dispatch! :auth [_ data]
  (edn-xhr {:url "/auth"
            :method :post
            :data {:email (data :email)
                   :password (data :password)
                   :csrf-token (:csrf-token @sync/chsk-state)}
            :on-error (fn [e]
                        ; TODO
                        )
            :on-complete (let [cred-auth? (data :email)]
                           (fn [data]
                             (if cred-auth?
                               (sync/reconnect!)
                               (start-session! (data :user-id)))))}))

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
  (store/add-users! (data :users))
  (store/add-messages! (data :messages)))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (dispatch! :auth {}))
