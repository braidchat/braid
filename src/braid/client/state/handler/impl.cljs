(ns braid.client.state.handler.impl
  (:require [clojure.string :as string]
            [braid.client.store :as store]
            [braid.client.sync :as sync]
            [braid.client.schema :as schema]
            [braid.common.util :as util]
            [braid.client.router :as router]
            [braid.client.xhr :refer [edn-xhr]]
            [braid.client.state.helpers :as helpers]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state.handler.core :refer [handler]]
            [braid.client.quests.handlers]))

(defn extract-tag-ids
  [text]
  (let [mentioned-names (->> (re-seq util/sigiled-tag-name-re text)
                             (map second))]
    (->> mentioned-names
         (map store/name->open-tag-id)
         (remove nil?))))

(defn extract-user-ids
  [text]
  (let [mentioned-names (->> (re-seq util/sigiled-nickname-re text)
                             (map second))
        nick->id (reduce (fn [m {:keys [id nickname]}] (assoc m nickname id))
                         {}
                         (store/all-users))]
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

(defmethod handler :clear-session [state _]
  (helpers/clear-session state))

(defmethod handler :new-message-text [state [_ {:keys [thread-id content]}]]
  (helpers/set-new-message state thread-id content))

(defmethod handler :display-error [state [_ args]]
  (apply helpers/display-error state args))

(defmethod handler :set-message-failed [state [_ message]]
  (helpers/set-message-failed state message))

(defmethod handler :new-message [state [_ data]]
  (if-not (string/blank? (data :content))
    (let [message (schema/make-message
                    {:user-id (helpers/current-user-id state)
                     :content (identify-mentions (data :content))
                     :thread-id (data :thread-id)
                     :group-id (data :group-id)
                     :mentioned-tag-ids (concat (data :mentioned-tag-ids)
                                                (extract-tag-ids (data :content)))
                     :mentioned-user-ids (concat (data :mentioned-user-ids)
                                                 (extract-user-ids (data :content)))})]
      (sync/chsk-send!
        [:braid.server/new-message message]
        2000
        (fn [reply]
          (when (not= :braid/ok reply)
            (dispatch! :display-error [(str :failed-to-send (message :id)) "Message failed to send!"])
            (dispatch! :set-message-failed message))))
      (-> state
          (helpers/add-message message)
          (helpers/maybe-reset-new-thread-id (data :thread-id))))
    state))

(defmethod handler :clear-error [state [_ err-key]]
  (helpers/clear-error state err-key))

(defmethod handler :resend-message [state [_ message]]
  (-> state
      (helpers/clear-error (str :failed-to-send (message :id)))
      (helpers/clear-message-failed message))
  (sync/chsk-send!
    [:braid.server/new-message message]
    2000
    (fn [reply]
      (when (not= :braid/ok reply)
        (dispatch! :display-error [(str :failed-to-send (message :id)) "Message failed to send!"])
        (dispatch! :set-message-failed message)))))

(defmethod handler :hide-thread [state [_ {:keys [thread-id local-only?]} ]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/hide-thread thread-id]))
  (helpers/hide-thread state thread-id))

(defmethod handler :reopen-thread [state [_ thread-id]]
  (sync/chsk-send! [:braid.server/show-thread thread-id])
  (helpers/show-thread state thread-id))

(defmethod handler :unsub-thread [state [_ data]]
  (sync/chsk-send! [:braid.server/unsub-thread (data :thread-id)])
  (handler state [:hide-thread {:thread-id (data :thread-id) :local-only? true}]))

(defmethod handler :create-tag [state [_ {:keys [tag local-only?]}]]
  (let [tag (merge (schema/make-tag) tag)]
    (when-not local-only?
      (sync/chsk-send!
        [:braid.server/create-tag tag]
        1000
        (fn [reply]
          (if-let [msg (:error reply)]
            (do
              (dispatch! :remove-tag {:tag-id (tag :id) :local-only? true})
              (dispatch! :display-error [(str :bad-tag (tag :id)) msg]))))))
    (-> state
        (helpers/add-tag tag)
        (helpers/subscribe-to-tag (tag :id)))))

(defmethod handler :unsubscribe-from-tag [state [_ tag-id]]
  (sync/chsk-send! [:braid.server/unsubscribe-from-tag tag-id])
  (helpers/unsubscribe-from-tag state tag-id))

(defmethod handler :subscribe-to-tag [state [_ {:keys [tag-id local-only?]}]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/subscribe-to-tag tag-id]))
  (helpers/subscribe-to-tag state tag-id))

(defmethod handler :set-tag-description [state [_ {:keys [tag-id description local-only?]}]]
  (when local-only?
    (sync/chsk-send!
      [:braid.server/set-tag-description {:tag-id tag-id :description description}]))
  (helpers/set-tag-description state tag-id description))

(defmethod handler :retract-tag [state [_ {:keys [tag-id local-only?]}]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/retract-tag tag-id]))
  (helpers/remove-tag state tag-id))

(defmethod handler :remove-group [state [_ group-id]]
  (helpers/remove-group state group-id))

(defmethod handler :create-group [state [_ group]]
  (let [group (schema/make-group group)]
    (sync/chsk-send!
      [:braid.server/create-group group]
      1000
      (fn [reply]
        (when-let [msg (reply :error)]
          (.error js/console msg)
          (dispatch! :display-error [(str :bad-group (group :id)) msg])
          (dispatch! :remove-group (group :id)))))
    (-> state
        (helpers/add-group group)
        (helpers/become-group-admin (:id group)))))

(defmethod handler :update-user-nickname [state [_ {:keys [nickname user-id]}]]
  (helpers/update-user-nickname state user-id nickname))

(defmethod handler :set-user-nickname [state [_ {:keys [nickname on-error]}]]
  (sync/chsk-send!
    [:braid.server/set-nickname {:nickname nickname}]
    1000
    (fn [reply]
      (if-let [msg (reply :error)]
        (on-error msg)
        (dispatch! :update-user-nickname
                   {:nickname nickname
                    :user-id (helpers/current-user-id state)}))))
  state)

(defmethod handler :set-user-avatar [state [_ avatar-url]]
  (sync/chsk-send! [:braid.server/set-user-avatar avatar-url])
  (helpers/update-user-avatar state (helpers/current-user-id state) avatar-url))

(defmethod handler :update-user-avatar [state [_ {:keys [user-id avatar-url]}]]
  (helpers/update-user-avatar state user-id avatar-url))

(defmethod handler :set-password [state [_ [password on-success on-error]]]
  (sync/chsk-send!
    [:braid.server/set-password {:password password}]
    3000
    (fn [reply]
      (cond
        (reply :error) (on-error reply)
        (= reply :chsk/timeout) (on-error
                                  {:error "Couldn't connect to server, please try again"})
        true (on-success))))
  state)

(defmethod handler :add-notification-rule [state [_ rule]]
  (let [current-rules (get (helpers/get-user-preferences state) :notification-rules [])]
    (handler state [:set-preference [:notification-rules (conj current-rules rule)]])))

(defmethod handler :remove-notification-rule [state [_ rule]]
  (let [new-rules (->> (get (helpers/get-user-preferences state) :notification-rules [])
                       (into [] (remove (partial = rule))))]
    (handler state [:set-preference [:notification-rules new-rules]])))

(defmethod handler :set-search-results [state [_ [query reply]]]
  (helpers/set-search-results state query reply))

(defmethod handler :set-search-query [state [_ query]]
  (helpers/set-search-query state query))

(defmethod handler :search-history [state [_ [query group-id]]]
  (if query
    (do (sync/chsk-send!
          [:braid.server/search [query group-id]]
          15000
          (fn [reply]
            (dispatch! :set-page-loading false)
            (if (:thread-ids reply)
              (dispatch! :set-search-results [query reply])
              (dispatch! :set-page-error true))))
        (helpers/set-page-error state false))
    state))

(defmethod handler :add-threads [state [_ threads]]
  (helpers/add-threads state threads))

(defmethod handler :load-recent-threads
  [state [_ {:keys [group-id on-error on-complete]}]]
  (sync/chsk-send!
    [:braid.server/load-recent-threads group-id]
    5000
    (fn [reply]
      (if-let [threads (:braid/ok reply)]
        (dispatch! :add-threads threads)
        (cond
          (= reply :chsk/timeout) (on-error "Timed out")
          (:braid/error reply) (on-error (:braid/error reply))
          :else (on-error "Something went wrong")))
      (on-complete (some? (:braid/ok reply)))))
  state)

(defmethod handler :load-threads [state [_ {:keys [thread-ids on-complete]}]]
  (sync/chsk-send!
    [:braid.server/load-threads thread-ids]
    5000
    (fn [reply]
      (when-let [threads (:threads reply)]
        (dispatch! :add-threads threads))
      (when on-complete
        (on-complete))))
  state)

(defmethod handler :set-channel-results [state [_ results]]
  (helpers/set-channel-results state results))

(defmethod handler :add-channel-results [state [_ results]]
  (helpers/add-channel-results state results))

(defmethod handler :set-pagination-remaining [state [_ threads-count]]
  (helpers/set-pagination-remaining state threads-count))

(defmethod handler :threads-for-tag [state [_ {:keys [tag-id offset limit on-complete]
                                               :or {offset 0 limit 25}}]]
  (sync/chsk-send!
    [:braid.server/threads-for-tag {:tag-id tag-id :offset offset :limit limit}]
    2500
    (fn [reply]
      (when-let [results (:threads reply)]
        (if (zero? offset)
          ; initial load of threads
          (dispatch! :set-channel-results results)
          ; paging more results in
          (dispatch! :add-channel-results results))
        (dispatch! :set-pagination-remaining (:remaining reply))
        (when on-complete (on-complete)))))
  state)

(defmethod handler :mark-thread-read [state [_ thread-id]]
  (sync/chsk-send! [:braid.server/mark-thread-read thread-id])
  (helpers/update-thread-last-open-at state thread-id))

(defmethod handler :focus-thread [state [_ thread-id]]
  (helpers/focus-thread state thread-id))

(defmethod handler :clear-inbox [state [_ _]]
  (->> (helpers/get-open-threads state)
       (map :id )
       (reduce (fn [state id]
                 (handler state [:hide-thread {:thread-id id :local-only? false}]))
               state)))

(defmethod handler :invite [state [_ data]]
  (let [invite (schema/make-invitation data)]
    (sync/chsk-send! [:braid.server/invite-to-group invite])
    state))

(defmethod handler :generate-link [state [_ {:keys [group-id expires complete]}]]
  (println "dispatching generate")
  (sync/chsk-send!
    [:braid.server/generate-invite-link {:group-id group-id :expires expires}]
    5000
    (fn [reply]
      ; indicate error if it fails?
      (when-let [link (:link reply)]
        (complete link))))
  state)

(defmethod handler :accept-invite [state [_ invite]]
  (sync/chsk-send! [:braid.server/invitation-accept invite])
  (helpers/remove-invite state invite))

(defmethod handler :decline-invite [state [_ invite]]
  (sync/chsk-send! [:braid.server/invitation-decline invite])
  (helpers/remove-invite state invite))

(defmethod handler :make-admin [state [_ {:keys [group-id user-id local-only?] :as args}]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/make-user-admin args]))
  (helpers/make-user-admin state user-id group-id))

(defmethod handler :remove-from-group [state [ _ {:keys [group-id user-id] :as args}]]
  (sync/chsk-send! [:braid.server/remove-from-group args])
  state)

(defmethod handler :set-group-intro [state [_ {:keys [group-id intro local-only?] :as args}]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/set-group-intro args]))
  (helpers/set-group-intro state group-id intro))

(defmethod handler :set-group-avatar [state [_ {:keys [group-id avatar local-only?] :as args}]]
  (when-not local-only?
    (sync/chsk-send! [:braid.server/set-group-avatar args]))
  (helpers/set-group-avatar state group-id avatar))

(defmethod handler :make-group-public! [state [_ group-id]]
  (sync/chsk-send! [:braid.server/set-group-publicity [group-id true]])
  state)

(defmethod handler :make-group-private! [state [_ group-id]]
  (sync/chsk-send! [:braid.server/set-group-publicity [group-id false]])
  state)

(defmethod handler :new-bot [state [_ {:keys [bot on-complete]}]]
  (let [bot (schema/make-bot bot)]
    (sync/chsk-send!
      [:braid.server/create-bot bot]
      5000
      (fn [reply]
        (when (nil? (:braid/ok reply))
          (dispatch! :display-error
                     [(str "bot-" (bot :id) (rand))
                      (get reply :braid/error "Something when wrong creating bot")]))
        (on-complete (:braid/ok reply)))))
  state)

(defmethod handler :get-bot-info [state [_ {:keys [bot-id on-complete]}]]
  (sync/chsk-send!
    [:braid.server/get-bot-info bot-id]
    2000
    (fn [reply]
      (when-let [bot (:braid/ok reply)]
        (on-complete bot))))
  state)

(defmethod handler :create-upload [state [_ {:keys [url thread-id group-id]}]]
  (sync/chsk-send! [:braid.server/create-upload
                    (schema/make-upload {:url url :thread-id thread-id})])
  (handler state [:new-message {:content url :thread-id thread-id :group-id group-id}]))

(defmethod handler :get-group-uploads [state [_ {:keys [group-id on-success on-error]}]]
  (sync/chsk-send!
    [:braid.server/uploads-in-group group-id]
    5000
    (fn [reply]
      (if-let [uploads (:braid/ok reply)]
        (on-success uploads)
        (on-error (get reply :braid/error "Couldn't get uploads in group")))))
 state)

(defmethod handler :check-auth [state _]
  (edn-xhr {:uri "/check"
            :method :get
            :on-complete (fn [_]
                           (dispatch! :start-socket))
            :on-error (fn [_]
                        (dispatch! :set-login-state :login-form))})
  state)

(defmethod handler :set-login-state [state [_ login-state]]
  (helpers/set-login-state state login-state))

(defmethod handler :start-socket [state [_ _]]
  (sync/make-socket!)
  (sync/start-router!)
  (helpers/set-login-state state :ws-connect))

(defmethod handler :set-window-visibility [state [_ visible?]]
  (helpers/set-window-visibility state visible?))

(defmethod handler :auth [state [_ data]]
  (edn-xhr {:uri "/auth"
            :method :post
            :params {:email (data :email)
                     :password (data :password)}
            :on-complete (fn [_]
                           (when-let [cb (data :on-complete)]
                             (cb))
                           (dispatch! :start-socket))
            :on-error (fn [_]
                        (when-let [cb (data :on-error)]
                          (cb)))})
  state)

(defmethod handler :request-reset [state [_ email]]
  (edn-xhr {:uri "/request-reset"
            :method :post
            :params {:email email}})
  state)

(defmethod handler :logout [state [_ _]]
  (edn-xhr {:uri "/logout"
            :method :post
            :params {:csrf-token (:csrf-token @sync/chsk-state)}
            :on-complete (fn [data]
                           (dispatch! :set-login-state :login-form)
                           (dispatch! :clear-session))})
  state)

(defmethod handler :set-group-and-page [state [_ [group-id page-id]]]
  (helpers/set-group-and-page state group-id page-id))

(defmethod handler :set-page-loading [state [_ bool]]
  (helpers/set-page-loading state bool))

(defmethod handler :set-page-error [state [_ bool]]
  (helpers/set-page-error state bool))

(defmethod handler :set-preference [state [_ [k v]]]
  (sync/chsk-send! [:braid.server/set-preferences {k v}])
  (helpers/set-preferences state {k v}))

(defmethod handler :add-users [state [_ users]]
  (helpers/add-users state users))

(defmethod handler :join-group [state [_ {:keys [group tags]}]]
  (let [state (-> state
                  (helpers/add-tags tags)
                  (helpers/add-group group))])
  (reduce (fn [s tag]
            (helpers/subscribe-to-tag s (tag :id)))
          state
          tags))

(defmethod handler :notify-if-client-out-of-date [state [_ server-checksum]]
  (if (not= (aget js/window "checksum") server-checksum)
    (handler state [:display-error [:client-out-of-date "Client out of date - please refresh" :info]])
    state))

(defmethod handler :set-init-data [state [_ data]]
  (-> state
      (helpers/set-login-state :app)
      (helpers/set-session {:user-id (data :user-id)})
      (helpers/add-users (data :users))
      (helpers/add-tags (data :tags))
      (helpers/set-subscribed-tag-ids (data :user-subscribed-tag-ids))
      (helpers/set-preferences (data :user-preferences))
      (helpers/set-groups (data :user-groups))
      (helpers/set-invitations (data :invitations))
      (helpers/set-threads (data :user-threads))
      (helpers/set-open-threads (data :user-threads))))

(defmethod handler :leave-group [state [_ {:keys [group-id group-name]}]]
  ; need to :remove-group before router is dispatched below
  (dispatch! :remove-group group-id)
  (when (= group-id (helpers/get-open-group-id state))
    (router/go-to "/"))
  (-> state
      (helpers/display-error (str "left-" group-id)
                             (str "You have been removed from " group-name)
                             :info)
      ; need this here also so the state gets changed when this dispatcher returns
      (helpers/remove-group group-id)
      ((fn [state]
         (if-let [sidebar-order (:groups-order (helpers/get-user-preferences state))]
           (helpers/set-preferences
             state
             {:groups-order (into [] (remove (partial = group-id))
                                  sidebar-order)})
           state)))))

(defmethod handler :add-open-thread [state [_ thread]]
  (helpers/add-open-thread state thread))

(defmethod handler :maybe-increment-unread [state _]
  (helpers/maybe-increment-unread state))

(defmethod handler :add-invite [state [_ invite]]
  (helpers/add-invite state invite))

(defmethod handler :update-user-status [state [_ [user-id status]]]
  (helpers/update-user-status state user-id status))

(defmethod handler :add-user [state [_ user]]
  (helpers/add-user state user))

(defmethod handler :remove-user-from-group [state [_ [group-id user-id]]]
  (helpers/remove-user-from-group state user-id group-id))

(defmethod handler :set-group-publicity [state [_ [group-id publicity]]]
  (helpers/set-group-publicity state group-id publicity))

(defmethod handler :add-group-bot [state [_ [group-id bot]]]
  (helpers/add-group-bot state group-id bot))
