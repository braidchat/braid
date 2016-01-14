(ns chat.client.dispatcher
  (:require [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]
            [cljs-utils.core :refer [edn-xhr]]))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (when-not (string/blank? (data :content))
    (let [message (schema/make-message {:user-id (get-in @store/app-state [:session :user-id])
                                        :content (data :content)
                                        :thread-id (data :thread-id)})]
      (store/add-message! message)
      (sync/chsk-send! [:chat/new-message message])
      (when-let [mentioned-names (->> (re-seq #"(?:^|\s)@(\S+)" (message :content))
                                      (map second))]
        (let [nick->id (reduce (fn [m [id {:keys [nickname]}]] (assoc m nickname id))
                               {}
                               (@store/app-state :users))
              mentioned (->> mentioned-names (map nick->id) (remove nil?))]
          (doseq [mention mentioned]
            (sync/chsk-send! [:thread/add-mention {:thread-id (message :thread-id)
                                                   :mentioned-id mention}])
            (store/add-mention-to-thread! mention (message :thread-id))))))))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:chat/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :create-tag [_ [tag-name group-id]]
  (let [tag (schema/make-tag {:name tag-name :group-id group-id})]
    (store/add-tag! tag)
    (sync/chsk-send! [:chat/create-tag tag])
    (dispatch! :subscribe-to-tag (tag :id))))

(defmethod dispatch! :unsubscribe-from-tag [_ tag-id]
  (sync/chsk-send! [:user/unsubscribe-from-tag tag-id])
  (store/unsubscribe-from-tag! tag-id))

(defmethod dispatch! :subscribe-to-tag [_ tag-id]
  (sync/chsk-send! [:user/subscribe-to-tag tag-id])
  (store/subscribe-to-tag! tag-id))

(defmethod dispatch! :tag-thread [_ attr]
  (when-let [tag-id (or (attr :id) (store/tag-id-for-name (attr :tag-name)))]
    (sync/chsk-send! [:thread/add-tag {:thread-id (attr :thread-id)
                                       :tag-id tag-id}])
    (store/add-tag-to-thread! tag-id (attr :thread-id))))

(defmethod dispatch! :mention-user [_ [thread-id user-id]]
  (sync/chsk-send! [:thread/add-mention {:thread-id thread-id
                                         :mentioned-id user-id}])
  (store/add-mention-to-thread! user-id thread-id))

(defmethod dispatch! :create-group [_ group]
  (let [group (schema/make-group group)]
    (sync/chsk-send!
      [:chat/create-group group]
      1000
      (fn [reply]
        (when-let [msg (reply :error)]
          (.error js/console msg)
          (store/display-error! msg)
          (store/remove-group! group))))
    (store/add-group! group)))

(defmethod dispatch! :set-nickname [_ [nickname on-error]]
  (sync/chsk-send!
    [:user/set-nickname {:nickname nickname}]
    1000
    (fn [reply]
      (if (reply :error)
        (on-error)
        (store/set-nickname! nickname)))))

(defmethod dispatch! :search-history [_ query]
  (sync/chsk-send!
    [:chat/search query]
    2000
    (fn [reply]
      (when-let [results (:threads reply)]
       (store/set-search-results! results)))))

(defmethod dispatch! :invite [_ data]
  (let [invite (schema/make-invitation data)]
    (sync/chsk-send! [:chat/invite-to-group invite])))

(defmethod dispatch! :accept-invite [_ invite]
  (sync/chsk-send! [:chat/invitation-accept invite])
  (store/remove-invite! invite))

(defmethod dispatch! :decline-invite [_ invite]
  (sync/chsk-send! [:chat/invitation-decline invite])
  (store/remove-invite! invite))

(defmethod dispatch! :auth [_ data]
  (edn-xhr {:url "/auth"
            :method :post
            :data {:email (data :email)
                   :password (data :password)
                   :csrf-token (:csrf-token @sync/chsk-state)}
            :on-error (fn [e]
                        (when-let [cb (data :on-error)]
                          (cb)))
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
  (store/set-session! {:user-id (data :user-id) :nickname (data :user-nickname)})
  (store/add-users! (data :users))
  (store/add-tags! (data :tags))
  (store/set-user-subscribed-tag-ids! (data :user-subscribed-tag-ids))
  (store/set-user-joined-groups! (data :user-groups))
  (store/set-invitations! (data :invitations))
  (store/set-threads! (data :user-threads)))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (sync/chsk-send! [:session/start nil]))

(defmethod sync/event-handler :chat/create-tag
  [[_ data]]
  (store/add-tag! data)
  (dispatch! :subscribe-to-tag (data :id)))

(defmethod sync/event-handler :chat/joined-group
  [[_ data]]
  (store/add-group! (data :group))
  (store/add-tags! (data :tags))
  (doseq [t (data :tags)]
    (store/subscribe-to-tag! (t :id))))

(defmethod sync/event-handler :chat/update-users
  [[_ data]]
  (store/add-users! data))

(defmethod sync/event-handler :chat/new-user
  [[_ user]]
  (store/add-user! user))

(defmethod sync/event-handler :chat/invitation-recieved
  [[_ invite]]
  (store/add-invite! invite))

(defmethod sync/event-handler :user/name-change
  [[_ {:keys [user-id nickname]}]]
  (store/update-user-nick! user-id nickname))
