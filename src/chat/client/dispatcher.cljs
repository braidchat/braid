(ns chat.client.dispatcher
  (:require [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]
            [chat.shared.util :as util]
            [chat.client.router :as router]
            [chat.client.routes :as routes]
            [chat.client.xhr :refer [edn-xhr]]
            [braid.desktop.notify :as notify]))

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

(defmethod dispatch! :new-message-text [_ {:keys [thread-id content]}]
  (store/set-new-message! thread-id content))

(defmethod dispatch! :new-message [_ data]
  (when-not (string/blank? (data :content))
    (let [message (schema/make-message
                    {:user-id (get-in @store/app-state [:session :user-id])
                     :content (identify-mentions (data :content))
                     :thread-id (data :thread-id)
                     :group-id (data :group-id)

                     :mentioned-tag-ids (concat (data :mentioned-tag-ids)
                                                (extract-tag-ids (data :content)))
                     :mentioned-user-ids (concat (data :mentioned-user-ids)
                                                 (extract-user-ids (data :content)))})]
      (store/add-message! message)
      (sync/chsk-send!
        [:braid.server/new-message message]
        2000
        (fn [reply]
          (when (not= :braid/ok reply)
            (store/display-error! (str :failed-to-send (message :id)) "Message failed to send!")
            (store/set-message-failed! message)))))))

(defmethod dispatch! :resend-message [_ message]
  (store/clear-error! (str :failed-to-send (message :id)))
  (store/clear-message-failed! message)
  (sync/chsk-send!
    [:braid.server/new-message message]
    2000
    (fn [reply]
      (when (not= :braid/ok reply)
        (store/display-error! (str :failed-to-send (message :id)) "Message failed to send!")
        (store/set-message-failed! message)))))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:braid.server/hide-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :unsub-thread [_ data]
  (sync/chsk-send! [:braid.server/unsub-thread (data :thread-id)])
  (store/hide-thread! (data :thread-id)))

(defmethod dispatch! :create-tag [_ [tag-name group-id id]]
  (let [tag (schema/make-tag {:name tag-name
                              :group-id group-id
                              :group-name (:name (store/id->group group-id))
                              :id id})]
    (store/add-tag! tag)
    (sync/chsk-send!
      [:braid.server/create-tag tag]
      1000
      (fn [reply]
        (if-let [msg (:error reply)]
          (do
            (store/remove-tag! (tag :id))
            (store/display-error! (str :bad-tag (tag :id)) msg))
          (dispatch! :subscribe-to-tag (tag :id)))))))

(defmethod dispatch! :unsubscribe-from-tag [_ tag-id]
  (sync/chsk-send! [:braid.server/unsubscribe-from-tag tag-id])
  (store/unsubscribe-from-tag! tag-id))

(defmethod dispatch! :subscribe-to-tag [_ tag-id]
  (sync/chsk-send! [:braid.server/subscribe-to-tag tag-id])
  (store/subscribe-to-tag! tag-id))

(defmethod dispatch! :set-tag-description [_ [tag-id desc]]
  (store/update-tag-description! tag-id desc)
  (sync/chsk-send!
    [:braid.server/set-tag-description {:tag-id tag-id :description desc}]))

(defmethod dispatch! :create-group [_ group]
  (let [group (schema/make-group group)]
    (sync/chsk-send!
      [:braid.server/create-group group]
      1000
      (fn [reply]
        (when-let [msg (reply :error)]
          (.error js/console msg)
          (store/display-error! (str :bad-group (group :id)) msg)
          (store/remove-group! group))))
    (store/add-group! group)
    (store/become-group-admin! (:id group))))

(defmethod dispatch! :set-nickname [_ [nickname on-error]]
  (sync/chsk-send!
    [:braid.server/set-nickname {:nickname nickname}]
    1000
    (fn [reply]
      (if-let [msg (reply :error)]
        (on-error msg)
        (store/update-user-nick! (store/current-user-id) nickname)))))

(defmethod dispatch! :set-user-avatar [_ avatar-url]
  (store/update-user-avatar! (store/current-user-id) avatar-url)
  (sync/chsk-send! [:braid.server/set-avatar avatar-url]))

(defmethod dispatch! :set-password [_ [password on-success on-error]]
  (sync/chsk-send!
    [:braid.server/set-password {:password password}]
    3000
    (fn [reply]
      (cond
        (reply :error) (on-error reply)
        (= reply :chsk/timeout) (on-error
                                  {:error "Couldn't connect to server, please try again"})
        true (on-success)))))

(defmethod dispatch! :set-preference [_ [k v]]
  (store/add-preferences! {k v})
  (sync/chsk-send! [:braid.server/set-preferences {k v}]))

(defmethod dispatch! :add-notification-rule [_ rule]
  (let [current-rules (get (store/user-preferences) :notification-rules [])]
    (dispatch! :set-preference [:notification-rules (conj current-rules rule)])))

(defmethod dispatch! :remove-notification-rule [_ rule]
  (let [new-rules (->> (get (store/user-preferences) :notification-rules [])
                       (into [] (remove (partial = rule))))]
    (dispatch! :set-preference [:notification-rules new-rules])))

(defmethod dispatch! :search-history [_ [query group-id]]
  (when query
    (store/clear-search-error!)
    (sync/chsk-send!
      [:braid.server/search [query group-id]]
      15000
      (fn [reply]
        (if (:thread-ids reply)
          (store/set-search-results! query reply)
          (store/set-search-error!))))))

(defmethod dispatch! :load-threads [_ {:keys [thread-ids on-complete]}]
  (sync/chsk-send!
    [:braid.server/load-threads thread-ids]
    5000
    (fn [reply]
      (when-let [threads (:threads reply)]
        (store/add-threads! threads))
      (when on-complete
        (on-complete)))))

(defmethod dispatch! :threads-for-tag [_ {:keys [tag-id offset limit on-complete]
                                          :or {offset 0 limit 25}}]
  (sync/chsk-send!
    [:braid.server/threads-for-tag {:tag-id tag-id :offset offset :limit limit}]
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
  (sync/chsk-send! [:braid.server/mark-thread-read thread-id]))

(defmethod dispatch! :invite [_ data]
  (let [invite (schema/make-invitation data)]
    (sync/chsk-send! [:braid.server/invite-to-group invite])))

(defmethod dispatch! :generate-link [_ {:keys [group-id expires complete]}]
  (println "dispatching generate")
  (sync/chsk-send!
    [:braid.server/generate-invite-link {:group-id group-id :expires expires}]
    5000
    (fn [reply]
      ; indicate error if it fails?
      (when-let [link (:link reply)]
        (complete link)))))

(defmethod dispatch! :accept-invite [_ invite]
  (sync/chsk-send! [:braid.server/invitation-accept invite])
  (store/remove-invite! invite))

(defmethod dispatch! :decline-invite [_ invite]
  (sync/chsk-send! [:braid.server/invitation-decline invite])
  (store/remove-invite! invite))

(defmethod dispatch! :make-admin [_ {:keys [group-id user-id] :as args}]
  (sync/chsk-send! [:braid.server/make-user-admin args])
  (store/add-group-admin! group-id user-id))

(defmethod dispatch! :remove-from-group [ _ {:keys [group-id user-id] :as args}]
  (sync/chsk-send! [:braid.server/remove-from-group args]))

(defmethod dispatch! :set-intro [_ {:keys [group-id intro] :as args}]
  (sync/chsk-send! [:braid.server/set-group-intro args])
  (store/set-group-intro! group-id intro))

(defmethod dispatch! :set-group-avatar [_ {:keys [group-id avatar] :as args}]
  (sync/chsk-send! [:braid.server/set-group-avatar args])
  (store/set-group-avatar! group-id avatar))

(defmethod dispatch! :make-group-public! [_ group-id]
  (sync/chsk-send! [:braid.server/set-group-publicity [group-id true]]))

(defmethod dispatch! :make-group-private! [_ group-id]
  (sync/chsk-send! [:braid.server/set-group-publicity [group-id false]]))

(defmethod dispatch! :new-bot [_ {:keys [bot on-complete]}]
  (let [bot (schema/make-bot bot)]
    (sync/chsk-send!
      [:braid.server/create-bot bot]
      5000
      (fn [reply]
        (when (nil? (:braid/ok reply))
          (store/display-error!
            (str "bot-" (bot :id) (rand))
            (get reply :braid/error "Something when wrong creating bot")))
        (on-complete (:braid/ok reply))))))

(defmethod dispatch! :get-bot-info [_ {:keys [bot-id on-complete]}]
  (sync/chsk-send!
    [:braid.server/get-bot-info bot-id]
    2000
    (fn [reply]
      (when-let [bot (:braid/ok reply)]
        (on-complete bot)))))

(defmethod dispatch! :check-auth! [_ _]
  (edn-xhr {:uri "/check"
            :method :get
            :on-complete (fn [_]
                           (dispatch! :start-socket!))
            :on-error (fn [_]
                        (dispatch! :set-login-state! :login-form))}))

(defmethod dispatch! :set-login-state! [_ state]
  (store/set-login-state! state))

(defmethod dispatch! :start-socket! [_ _]
  (dispatch! :set-login-state! :ws-connect)
  (sync/make-socket!)
  (sync/start-router!))

(defmethod dispatch! :auth [_ data]
  (edn-xhr {:uri "/auth"
            :method :post
            :params {:email (data :email)
                     :password (data :password)}
            :on-complete (fn [_]
                           (when-let [cb (data :on-complete)]
                             (cb))
                           (dispatch! :start-socket!))
            :on-error (fn [_]
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
                           (dispatch! :set-login-state! :login-form)
                           (store/clear-session!))}))

(defmethod dispatch! :clear-inbox [_ _]
  (let [open-thread-ids (map :id (store/open-threads @store/app-state))]
    (doseq [id open-thread-ids]
      (dispatch! :hide-thread {:thread-id id}))))

(defn check-client-version [server-checksum]
  (when (not= (aget js/window "checksum") server-checksum)
    (store/display-error! :client-out-of-date "Client out of date - please refresh" :info)))

; Websocket Events

(defmethod sync/event-handler :chat/thread
  [[_ data]]
  (store/add-open-thread! data))

(defmethod sync/event-handler :session/init-data
  [[_ data]]
  (dispatch! :set-login-state! :app)
  (check-client-version (data :version-checksum))
  (store/set-session! {:user-id (data :user-id)})
  (store/add-users! (data :users))
  (store/add-tags! (data :tags))
  (store/set-user-subscribed-tag-ids! (data :user-subscribed-tag-ids))
  (store/add-preferences! (data :user-preferences))
  (store/set-user-joined-groups! (data :user-groups))
  (store/set-invitations! (data :invitations))
  (store/set-open-threads! (data :user-threads))
  (router/dispatch-current-path!))

(defmethod sync/event-handler :socket/connected
  [[_ _]]
  (sync/chsk-send! [:braid.server/start nil]))

(defmethod sync/event-handler :chat/create-tag
  [[_ data]]
  (store/add-tag! data)
  (store/subscribe-to-tag! (data :id)))

(defmethod sync/event-handler :chat/joined-group
  [[_ data]]
  (store/add-group! (data :group))
  (store/add-tags! (data :tags))
  (doseq [t (data :tags)]
    (store/subscribe-to-tag! (t :id))))

(defmethod sync/event-handler :chat/update-users
  [[_ data]]
  (store/add-users! data))

(defmethod sync/event-handler :chat/invitation-received
  [[_ invite]]
  (store/add-invite! invite))

(defmethod sync/event-handler :user/name-change
  [[_ {:keys [user-id nickname]}]]
  (store/update-user-nick! user-id nickname))

(defmethod sync/event-handler :user/new-avatar
  [[_ {:keys [user-id avatar]}]]
  (store/update-user-avatar! user-id avatar))

(defmethod sync/event-handler :user/left-group
  [[_ [group-id group-name]]]
  (store/remove-group! {:id group-id})
  (store/display-error!
    (str "left-" group-id)
    (str "You have been removed from " group-name)
    :info)
  (when-let [sidebar-order (:groups-order (store/user-preferences))]
    (store/add-preferences!
      {:groups-order (into [] (remove (partial = group-id))
                           sidebar-order)}))
  (when (= group-id (store/open-group-id))
    (routes/go-to! (routes/index-path))))

(defmethod sync/event-handler :user/connected
  [[_ user-id]]
  (store/update-user-status! user-id :online))

(defmethod sync/event-handler :user/disconnected
  [[_ user-id]]
  (store/update-user-status! user-id :offline))

(defmethod sync/event-handler :group/new-user
  [[_ user]]
  (store/add-user! (assoc user :status :online)))

(defmethod sync/event-handler :group/user-left
  [[_ [group-id user-id]]]
  (store/remove-user-group! user-id group-id))

(defmethod sync/event-handler :group/new-admin
  [[_ [group-id new-admin-id]]]
  (store/add-group-admin! group-id new-admin-id))

(defmethod sync/event-handler :group/tag-descrption-change
  [[_ [tag-id new-description]]]
  (store/update-tag-description! tag-id new-description))

(defmethod sync/event-handler :group/new-intro
  [[_ [group-id intro]]]
  (store/set-group-intro! group-id intro))

(defmethod sync/event-handler :group/new-avatar
  [[_ [group-id avatar]]]
  (store/set-group-avatar! group-id avatar))

(defmethod sync/event-handler :group/publicity-changed
  [[_ [group-id publicity]]]
  (store/set-group-publicity! group-id publicity))

(defmethod sync/event-handler :group/new-bot
  [[_ [group-id bot]]]
  (store/add-group-bot! group-id bot))

(defmethod sync/event-handler :chat/notify-message
  [[_ message]]
  (notify/notify {:msg (:content message)}))

(defmethod sync/event-handler :chat/hide-thread
  [[_ thread-id]]
  (store/hide-thread! thread-id))
