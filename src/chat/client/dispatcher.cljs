(ns chat.client.dispatcher
  (:require [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]
            [chat.shared.util :as util]
            [chat.client.router :as router]
            [chat.client.xhr :refer [edn-xhr]]))

(defn- extract-tag-ids [text]
  (let [mentioned-names (->> (re-seq util/sigiled-tag-name-re text)
                             (map second))]
    (->> mentioned-names
         (map store/name->open-tag-id)
         (remove nil?))))

(defn- extract-user-ids [text]
  (let [mentioned-names (->> (re-seq util/sigiled-nickname-re text)
                             (map second))
        nick->id (reduce (fn [m [id {:keys [nickname]}]] (assoc m nickname id))
                         {}
                         (@store/app-state :users))]
    (->> mentioned-names
         (map nick->id)
         (filter store/user-in-open-group?)
         (remove nil?))))

(defn identify-mentions
  [content]
  (-> content
      (string/replace util/sigiled-nickname-re
                      (fn [[m nick]]
                        ; sometimes need leading whitespace, because javascript regex doesn't have lookbehind
                        (str (second (re-matches #"^(\s).*" m))
                             "@"
                             (if-let [user-id (:id (store/nickname->user nick))]
                                   (if (store/user-in-open-group? user-id)
                                     user-id
                                     nick)
                                   nick))))
      (string/replace util/sigiled-tag-name-re
                      (fn [[m tag-name]]
                        ; sometimes need leading whitespace, because javascript regex doesn't have lookbehind
                        (str (second (re-matches #"^(\s).*" m))
                             "#" (or (store/name->open-tag-id tag-name)
                                      tag-name))))))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (when-not (string/blank? (data :content))
    (let [message (schema/make-message
                    {:user-id (get-in @store/app-state [:session :user-id])
                     :content (identify-mentions (data :content))
                     :thread-id (data :thread-id)

                     :mentioned-tag-ids (concat (data :mentioned-tag-ids)
                                                (extract-tag-ids (data :content)))
                     :mentioned-user-ids (concat (data :mentioned-user-ids)
                                                 (extract-user-ids (data :content)))})]
      (store/add-message! message)
      (sync/chsk-send!
        [:chat/new-message message]
        2000
        (fn [reply]
          (when (not= :braid/ok reply)
            (store/display-error! (str :failed-to-send (message :id)) "Message failed to send!")
            (store/set-message-failed! message)))))))

(defmethod dispatch! :resend-message [_ message]
  (store/clear-message-failed! message)
  (sync/chsk-send!
    [:chat/new-message message]
    2000
    (fn [reply]
      (when (not= :braid/ok reply)
        (store/display-error! (str :failed-to-send (message :id)) "Message failed to send!")
        (store/set-message-failed! message)))))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:chat/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :create-tag [_ [tag-name group-id]]
  (let [tag (schema/make-tag {:name tag-name :group-id group-id})]
    (store/add-tag! tag)
    (sync/chsk-send!
      [:chat/create-tag tag]
      1000
      (fn [reply]
        (if-let [msg (:error reply)]
          (do
            (store/remove-tag! (tag :id))
            (store/display-error! (str :bad-tag (tag :id)) msg))
          (dispatch! :subscribe-to-tag (tag :id)))))))

(defmethod dispatch! :unsubscribe-from-tag [_ tag-id]
  (sync/chsk-send! [:user/unsubscribe-from-tag tag-id])
  (store/unsubscribe-from-tag! tag-id))

(defmethod dispatch! :subscribe-to-tag [_ tag-id]
  (sync/chsk-send! [:user/subscribe-to-tag tag-id])
  (store/subscribe-to-tag! tag-id))

(defmethod dispatch! :create-group [_ group]
  (let [group (schema/make-group group)]
    (sync/chsk-send!
      [:chat/create-group group]
      1000
      (fn [reply]
        (when-let [msg (reply :error)]
          (.error js/console msg)
          (store/display-error! (str :bad-group (group :id)) msg)
          (store/remove-group! group))))
    (store/add-group! group)))

(defmethod dispatch! :set-nickname [_ [nickname on-error]]
  (sync/chsk-send!
    [:user/set-nickname {:nickname nickname}]
    1000
    (fn [reply]
      (if-let [msg (reply :error)]
        (on-error msg)
        (store/set-nickname! nickname)))))

(defmethod dispatch! :search-history [_ query]
  (sync/chsk-send!
    [:chat/search query]
    2500
    (fn [reply]
      (when (seq (:thread-ids reply))
        (store/set-search-results! reply)))))

(defmethod dispatch! :threads-for-tag [_ {:keys [tag-id offset limit on-complete]
                                          :or {offset 0 limit 25}}]
  (sync/chsk-send!
    [:chat/threads-for-tag {:tag-id tag-id :offset offset :limit limit}]
    2500
    (fn [reply]
      (when-let [results (:threads reply)]
        (if (zero? offset)
          ; initial load of threads
          (store/set-channel-results! results)
          ; paging more results in
          (store/add-channel-results! results))
        (store/set-pagination-remaining! (:remaining reply))
        (when on-complete (on-complete))))))

(defmethod dispatch! :mark-thread-read [_ thread-id]
  (store/update-thread-last-open-at thread-id)
  (sync/chsk-send! [:chat/mark-thread-read thread-id]))

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
  (edn-xhr {:uri "/auth"
            :method :post
            :params {:email (data :email)
                     :password (data :password)
                     :csrf-token (:csrf-token @sync/chsk-state)}
            :on-complete (fn [data]
                           (sync/reconnect!))
            :on-error (fn [data]
                        (when-let [cb (data :on-error)]
                          (cb)))}))

(defmethod dispatch! :request-reset [_ email]
  (edn-xhr {:uri "/request-reset"
            :method :post
            :params {:email email}}))

(defmethod dispatch! :logout [_ _]
  (edn-xhr {:uri "/logout"
            :method :post
            :params {:csrf-token (:csrf-token @sync/chsk-state)}
            :on-complete (fn [data]
                           (store/clear-session!))}))

(defmethod dispatch! :clear-inbox [_ _]
  (let [open-thread-ids (map :id (store/open-threads @store/app-state))]
    (doseq [id open-thread-ids]
      (dispatch! :hide-thread {:thread-id id}))))

(defn check-client-version [server-checksum]
  (when (not= (aget js/window "checksum") server-checksum)
    (store/display-error! :client-out-of-date "Client out of date - please refresh")))

; Websocket Events

(defmethod sync/event-handler :chat/thread
  [[_ data]]
  (store/add-open-thread! data))

(defmethod sync/event-handler :session/init-data
  [[_ data]]
  (check-client-version (data :version-checksum))
  (store/set-session! {:user-id (data :user-id) :nickname (data :user-nickname)})
  (store/add-users! (data :users))
  (store/add-tags! (data :tags))
  (store/set-user-subscribed-tag-ids! (data :user-subscribed-tag-ids))
  (store/set-user-joined-groups! (data :user-groups))
  (store/set-invitations! (data :invitations))
  (store/set-open-threads! (data :user-threads))
  (router/dispatch-current-path!))

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

(defmethod sync/event-handler :user/connected
  [[_ user-id]]
  (store/update-user-status! user-id :online))

(defmethod sync/event-handler :user/disconnected
  [[_ user-id]]
  (store/update-user-status! user-id :offline))
