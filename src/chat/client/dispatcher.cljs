(ns chat.client.dispatcher
  (:require [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]
            [cljs-utils.core :refer [edn-xhr]]))

(defmulti dispatch! (fn [event data] event))

; TODO: keep track of what failed to resend when reconnected? How do we detect reconnection?
(defn send!
  "Wrap sync/chsk-send! with timeout & error handling"
  [data]
  (sync/chsk-send!
    data 2000
    (fn [reply]
      (when-not (sync/cb-success? reply)
        (sync/event-handler [:socket/disconnected reply data])))))

(def tag-pattern
  "Pattern to extract tags.  Would prefer for the first subpattern to be a
  zero-width assertion, but javascript doesn't support lookbehind. The last
  subpattern needs to be zero-width so that two adjacent tags will be removed
  for the tagless text. The tag name itself is the first capture group."
  #"(?:^|\s)#(\S+)(?=\s|$)")

(defmethod dispatch! :new-message [_ data]
  (let [text (data :content)
        tags (->> (re-seq tag-pattern text) (map second))
        tagless-text (string/replace text tag-pattern "")
        new-thread? (nil? (data :thread-id))
        data (update data :thread-id #(or % (uuid/make-random-squuid)))]
    (when (or new-thread? (not (string/blank? tagless-text)))
      (let [message (schema/make-message {:user-id (get-in @store/app-state [:session :user-id])
                                          :content tagless-text
                                          :thread-id (data :thread-id)})]
        (store/add-message! message)
        (send! [:chat/new-message message])))
    (when-not (empty? tags)
      (doseq [tag tags]
        (dispatch! :tag-thread {:thread-id (data :thread-id)
                                :tag-name tag})))))

(defmethod dispatch! :hide-thread [_ data]
  (send! [:chat/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :create-tag [_ tag-name]
  (let [tag (schema/make-tag {:name tag-name})]
    (store/add-tag! tag)
    (send! [:chat/create-tag tag])
    (dispatch! :subscribe-to-tag (tag :id))))

(defmethod dispatch! :unsubscribe-from-tag [_ tag-id]
  (send! [:user/unsubscribe-from-tag tag-id])
  (store/unsubscribe-from-tag! tag-id))

(defmethod dispatch! :subscribe-to-tag [_ tag-id]
  (send! [:user/subscribe-to-tag tag-id])
  (store/subscribe-to-tag! tag-id))

(defmethod dispatch! :tag-thread [_ attr]
  (when-let [tag-id (store/tag-id-for-name (attr :tag-name))]
    (send! [:thread/add-tag {:thread-id (attr :thread-id)
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

(defmethod sync/event-handler :chat/thread
  [[_ data]]
  (store/add-thread! data))

(defmethod sync/event-handler :session/init-data
  [[_ data]]
  (store/set-session! {:user-id (data :user-id)})
  (store/add-users! (data :users))
  (store/add-tags! (data :tags))
  (store/set-user-subscribed-tag-ids! (data :user-subscribed-tag-ids))
  (store/set-threads! (data :user-threads)))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (store/clear-connection-error!)
  (sync/chsk-send! [:session/start nil]))

(defmethod sync/event-handler :socket/disconnected
  [[_ reply data]]
  (println "disconnected" reply data)
  ; TODO: indicate which change has failed?
  ; TODO: attempt to reconnect?
  (store/set-connection-error!))

(defmethod sync/event-handler :chat/create-tag
  [[_ data]]
  (store/add-tag! data))
