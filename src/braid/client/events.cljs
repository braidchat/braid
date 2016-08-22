(ns braid.client.events
  (:require [clojure.string :as string]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [braid.client.store :as store]
            [braid.client.sync :as sync]
            [braid.client.schema :as schema]
            [braid.common.util :as util]
            [braid.client.router :as router]
            [braid.client.xhr :refer [edn-xhr]]
            [braid.client.state.helpers :as helpers]
            [braid.client.quests.helpers :as quest-helpers]))

; TODO: handle callbacks declaratively too?
(reg-fx :websocket-send (fn [args] (apply sync/chsk-send! args)))

; TODO: handle callbacks declaratively too?
(reg-fx :edn-xhr (fn [args] (edn-xhr args)))

(reg-fx :redirect-to (fn [route] (router/go-to route)))

(reg-fx :window-title (fn [title] (set! (.-title js/document) title)))

(defn name->open-tag-id
  "Lookup tag by name in the open group"
  [state tag-name]
  (let [open-group (state :open-group-id)]
    (->> (state :tags)
         vals
         (filter (fn [t] (and (= open-group (t :group-id)) (= tag-name (t :name)))))
         first
         :id)))

(defn extract-tag-ids
  [state text]
  (let [mentioned-names (->> (re-seq util/sigiled-tag-name-re text)
                             (map second))]
    (->> mentioned-names
         (map (partial name->open-tag-id state))
         (remove nil?))))

(defn all-users
  [state]
  (vals (get-in state [:users])))

(defn user-in-open-group?
  [state user-id]
  (contains? (set (get-in state [:users user-id :group-ids]))
             (state :open-group-id)))

(defn extract-user-ids
  [state text]
  (let [mentioned-names (->> (re-seq util/sigiled-nickname-re text)
                             (map second))
        nick->id (reduce (fn [m {:keys [id nickname]}] (assoc m nickname id))
                         {}
                         (all-users state))]
    (->> mentioned-names
         (map nick->id)
         (filter (partial user-in-open-group? state))
         (remove nil?))))

(defn nickname->user
  [state nickname]
  (->> (get-in state [:users])
       vals
       (filter (fn [u] (= nickname (u :nickname))))
       ; nicknames are unique, so take the first
       first))

(defn identify-mentions
  [state content]
  (-> content
      (string/replace util/sigiled-nickname-re
                      (fn [[m nick]]
                        ; sometimes need leading whitespace, because javascript
                        ; regex doesn't have lookbehind
                        (str (second (re-matches #"^(\s).*" m))
                             "@"
                             (if-let [user-id (:id (nickname->user state nick))]
                                   (if (user-in-open-group? state user-id)
                                     user-id
                                     nick)
                                   nick))))
      (string/replace util/sigiled-tag-name-re
                      (fn [[m tag-name]]
                        ; sometimes need leading whitespace, because javascript
                        ; regex doesn't have lookbehind
                        (str (second (re-matches #"^(\s).*" m))
                             "#" (or (name->open-tag-id state tag-name)
                                      tag-name))))))

(reg-event-db
  :initialize-db
  (fn [_ _]
    store/initial-state))

(reg-event-db
  :clear-session
  (fn [state _]
    (helpers/clear-session state)))

(reg-event-db
  :new-message-text
  (fn [state [_ {:keys [thread-id content]}]]
    (helpers/set-new-message state thread-id content)))

(reg-event-db
  :display-error
  (fn [state [_ args]]
    (apply helpers/display-error state args)))

(reg-event-db
  :set-message-failed
  (fn [state [_ message]]
    (helpers/set-message-failed state message)))

(reg-event-fx
  :new-message
  (fn [{state :db :as cofx} [_ data]]
    (if-not (string/blank? (data :content))
      (let [message (schema/make-message
                      {:user-id (helpers/current-user-id state)
                       :content (identify-mentions state (data :content))
                       :thread-id (data :thread-id)
                       :group-id (data :group-id)
                       :mentioned-tag-ids (concat
                                            (data :mentioned-tag-ids)
                                            (extract-tag-ids state (data :content)))
                       :mentioned-user-ids (concat
                                             (data :mentioned-user-ids)
                                             (extract-user-ids state (data :content)))})]
        {:websocket-send
         (list
           [:braid.server/new-message message]
           2000
           (fn [reply]
             (when (not= :braid/ok reply)
               ; TODO
               (dispatch [:display-error [(str :failed-to-send (message :id))
                                          "Message failed to send!"]])
               ; TODO
               (dispatch [:set-message-failed message]))))
         :db (-> state
                 (helpers/add-message message)
                 (helpers/maybe-reset-new-thread-id (data :thread-id)))}))))

(reg-event-db
  :clear-error
  (fn [state [_ err-key]]
    (helpers/clear-error state err-key)))

(reg-event-fx
  :resend-message
  (fn [{state :db :as cofx} [_ message]]
    {:websocket-send (list
                       [:braid.server/new-message message]
                       2000
                       (fn [reply]
                         (when (not= :braid/ok reply)
                           (dispatch [:display-error
                                      [(str :failed-to-send (message :id))
                                       "Message failed to send!"]])
                           (dispatch [:set-message-failed message]))))

     :db (-> state
             (helpers/clear-error (str :failed-to-send (message :id)))
             (helpers/clear-message-failed message))}))

(reg-event-fx
  :hide-thread
  (fn [{state :db :as cofx} [_ {:keys [thread-id local-only?]}]]
    (merge
      {:db (helpers/hide-thread state thread-id)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/hide-thread thread-id])}))))

(reg-event-fx
  :reopen-thread
  (fn [{state :db :as cofx} [_ thread-id]]
    {:websocket-send (list [:braid.server/show-thread thread-id])
     :db (helpers/show-thread state thread-id)}))

(reg-event-fx
  :unsub-thread
  (fn [cofx [_ data]]
    {:websocket-send (list [:braid.server/unsub-thread (data :thread-id)])
     :dispatch [:hide-thread {:thread-id (data :thread-id) :local-only? true}]}))

(reg-event-fx
  :create-tag
  (fn [{state :db :as cofx} [_ {:keys [tag local-only?]}]]
    (let [tag (merge (schema/make-tag) tag)]
      (merge
        {:db (-> state
                 (helpers/add-tag tag)
                 (helpers/subscribe-to-tag (tag :id)))}
        (when-not local-only?
          {:websocket-send
           (list
             [:braid.server/create-tag tag]
             1000
             (fn [reply]
               (if-let [msg (:error reply)]
                 (do
                   (dispatch [:remove-tag {:tag-id (tag :id) :local-only? true}])
                   (dispatch [:display-error [(str :bad-tag (tag :id)) msg]])))))})))))

(reg-event-fx
  :unsubscribe-from-tag
  (fn [{state :db :as cofx} [_ tag-id]]
    {:websocket-send (list [:braid.server/unsubscribe-from-tag tag-id])
     :db (helpers/unsubscribe-from-tag state tag-id)}))

(reg-event-fx
  :subscribe-to-tag
  (fn [{state :db :as cofx} [_ {:keys [tag-id local-only?]}]]
    (merge
      {:db (helpers/subscribe-to-tag state tag-id)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/subscribe-to-tag tag-id])}))))

(reg-event-fx
  :set-tag-description
  (fn [{state :db :as cofx} [_ {:keys [tag-id description local-only?]}]]
    (merge
      {:db (helpers/set-tag-description state tag-id description)}
      (when-not local-only?
        {:websocket-send
         (list
           [:braid.server/set-tag-description {:tag-id tag-id
                                               :description description}])}))))

(reg-event-fx
  :remove-tag
  (fn [{state :db :as cofx} [_ {:keys [tag-id local-only?]}]]
    (merge
      {:db (helpers/remove-tag state tag-id)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/retract-tag tag-id])}))))

(reg-event-db
  :remove-group
  (fn [state [_ group-id]]
    (helpers/remove-group state group-id)))

(reg-event-fx
  :create-group
  (fn [{state :db :as cofx} [_ group]]
    (let [group (schema/make-group group)]
      {:websocket-send
       (list
         [:braid.server/create-group group]
         1000
         (fn [reply]
           (when-let [msg (reply :error)]
             (.error js/console msg)
             (dispatch [:display-error [(str :bad-group (group :id)) msg]])
             (dispatch [:remove-group (group :id)]))))
       :db (-> state
               (helpers/add-group group)
               (helpers/become-group-admin (:id group)))})))

(reg-event-db
  :update-user-nickname
  (fn [state [_ {:keys [nickname user-id]}]]
    (helpers/update-user-nickname state user-id nickname)))

(reg-event-fx
  :set-user-nickname
  (fn [{state :db :as cofx} [_ {:keys [nickname on-error]}]]
    {:websocket-send
     (list
       [:braid.server/set-nickname {:nickname nickname}]
       1000
       (fn [reply]
         (if-let [msg (reply :error)]
           (on-error msg)
           (dispatch [:update-user-nickname
                      {:nickname nickname
                       :user-id (helpers/current-user-id state)}]))))}))

(reg-event-fx
  :set-user-avatar
  (fn [{state :db :as cofx} [_ avatar-url]]
    {:websocket-send (list [:braid.server/set-user-avatar avatar-url])
     :db (helpers/update-user-avatar
           state (helpers/current-user-id state) avatar-url)}))

(reg-event-db
  :update-user-avatar
  (fn [state [_ {:keys [user-id avatar-url]}]]
    (helpers/update-user-avatar state user-id avatar-url)))

(reg-event-fx
  :set-password
  (fn [cofx [_ [password on-success on-error]]]
    {:websocket-send
     (list
       [:braid.server/set-password {:password password}]
       3000
       (fn [reply]
         (cond
           (reply :error) (on-error reply)
           (= reply :chsk/timeout)
           (on-error
             {:error "Couldn't connect to server, please try again"})
           true (on-success))))}))

(reg-event-fx
  :add-notification-rule
  (fn [{state :db :as cofx} [_ rule]]
    (let [current-rules (get (helpers/get-user-preferences state)
                          :notification-rules [])]
      {:dispatch [:set-preference [:notification-rules (conj current-rules rule)]]})))

(reg-event-fx
  :remove-notification-rule
  (fn [{state :db :as cofx} [_ rule]]
    (let [new-rules (into [] (remove (partial = rule))
                          (get (helpers/get-user-preferences state)
                            :notification-rules []))]
      {:dispatch [:set-preference [:notification-rules new-rules]]})))

(reg-event-db
  :set-search-results
  (fn [state [_ [query reply]]]
    (helpers/set-search-results state query reply)))

(reg-event-db
  :set-search-query
  (fn [state [_ query]]
    (helpers/set-search-query state query)))

(reg-event-fx
  :search-history
  (fn [{state :db :as cofx} [_ [query group-id]]]
    (if query
      {:websocket-send
       (list
         [:braid.server/search [query group-id]]
         15000
         (fn [reply]
           (dispatch [:set-page-loading false])
           (if (:thread-ids reply)
             (dispatch [:set-search-results [query reply]])
             (dispatch [:set-page-error true]))))
       :db (helpers/set-page-error state false)}
      {})))

(reg-event-db
  :add-threads
  (fn [state [_ threads]]
    (helpers/add-threads state threads)))

(reg-event-fx
  :load-recent-threads
  (fn [cofx [_ {:keys [group-id on-error on-complete]}]]
    {:websocket-send
     (list
       [:braid.server/load-recent-threads group-id]
       5000
       (fn [reply]
         (if-let [threads (:braid/ok reply)]
           (dispatch [:add-threads threads])
           (cond
             (= reply :chsk/timeout) (on-error "Timed out")
             (:braid/error reply) (on-error (:braid/error reply))
             :else (on-error "Something went wrong")))
         (on-complete (some? (:braid/ok reply)))))}))

(reg-event-fx
  :load-threads
  (fn [cofx [_ {:keys [thread-ids on-complete]}]]
    {:websocket-send
     (list
       [:braid.server/load-threads thread-ids]
       5000
       (fn [reply]
         (when-let [threads (:threads reply)]
           (dispatch [:add-threads threads]))
         (when on-complete
           (on-complete))))}))

(reg-event-db
  :set-channel-results
  (fn [state [_ results]]
    (helpers/set-channel-results state results)))

(reg-event-db
  :add-channel-results
  (fn [state [_ results]]
    (helpers/add-channel-results state results)))

(reg-event-db
  :set-pagination-remaining
  (fn [state [_ threads-count]]
    (helpers/set-pagination-remaining state threads-count)))

(reg-event-fx
  :threads-for-tag
  (fn [cofx [_ {:keys [tag-id offset limit on-complete]
                 :or {offset 0 limit 25}}]]
    {:websocket-send
     (list
       [:braid.server/threads-for-tag {:tag-id tag-id :offset offset :limit limit}]
       2500
       (fn [reply]
         (when-let [results (:threads reply)]
           (if (zero? offset)
             ; initial load of threads
             (dispatch [:set-channel-results results])
             ; paging more results in
             (dispatch [:add-channel-results results]))
           (dispatch [:set-pagination-remaining (:remaining reply)])
           (when on-complete (on-complete)))))}))

(reg-event-fx
  :mark-thread-read
  (fn [{state :db :as cofx} [_ thread-id]]
    {:websocket-send (list [:braid.server/mark-thread-read thread-id])
     :db (helpers/update-thread-last-open-at state thread-id)}))

(reg-event-db
  :focus-thread
  (fn [state [_ thread-id]]
    (helpers/focus-thread state thread-id)))

(reg-event-fx
  :clear-inbox
  (fn [{state :db :as cofx} [_ _]]
    {:dispatch-n
     (into ()
           (comp (map :id)
                 (map (fn [id] [:hide-thread {:thread-id id :local-only? false}])))
           (helpers/get-open-threads state))}))

(reg-event-fx
  :invite
  (fn [cofx [_ data]]
    (let [invite (schema/make-invitation data)]
      {:websocket-send (list [:braid.server/invite-to-group invite])})))

(reg-event-fx
  :generate-link
  (fn [cofx [_ {:keys [group-id expires complete]}]]
    {:websocket-send
     (list
       [:braid.server/generate-invite-link {:group-id group-id :expires expires}]
       5000
       (fn [reply]
         ; indicate error if it fails?
         (when-let [link (:link reply)]
           (complete link))))}))

(reg-event-fx
  :accept-invite
  (fn [{state :db :as cofx} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-accept invite])
     :db (helpers/remove-invite state invite)}))

(reg-event-fx
  :decline-invite
  (fn [{state :db :as cofx} [_ invite]]
    {:websocket-send (list [:braid.server/invitation-decline invite])
     :db (helpers/remove-invite state invite)}))

(reg-event-fx
  :make-admin
  (fn [{state :db :as cofx} [_ {:keys [group-id user-id local-only?] :as args}]]
    (merge
      {:db (helpers/make-user-admin state user-id group-id)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/make-user-admin args])}))))

(reg-event-fx
  :remove-from-group
  (fn [cofx [ _ {:keys [group-id user-id] :as args}]]
    {:websocket-send (list [:braid.server/remove-from-group args])}))

(reg-event-fx
  :set-group-intro
  (fn [{state :db :as cofx} [_ {:keys [group-id intro local-only?] :as args}]]
    (merge
      {:db (helpers/set-group-intro state group-id intro)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/set-group-intro args])}))))

(reg-event-fx
  :set-group-avatar
  (fn [{state :db :as cofx} [_ {:keys [group-id avatar local-only?] :as args}]]
    (merge
      {:db (helpers/set-group-avatar state group-id avatar)}
      (when-not local-only?
        {:websocket-send (list [:braid.server/set-group-avatar args])}))))

(reg-event-fx
  :make-group-public!
  (fn [cofx [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id true]])}))

(reg-event-fx
  :make-group-private!
  (fn [cofx [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id false]])}))

(reg-event-fx
  :new-bot
  (fn [cofx [_ {:keys [bot on-complete]}]]
    (let [bot (schema/make-bot bot)]
      {:websocket-send
       (list
         [:braid.server/create-bot bot]
         5000
         (fn [reply]
           (when (nil? (:braid/ok reply))
             (dispatch [:display-error
                        [(str "bot-" (bot :id) (rand))
                         (get reply :braid/error
                           "Something when wrong creating bot")]]))
           (on-complete (:braid/ok reply))))})))

(reg-event-fx
  :get-bot-info
  (fn [cofx [_ {:keys [bot-id on-complete]}]]
    {:websocket-send
     (list
       [:braid.server/get-bot-info bot-id]
       2000
       (fn [reply]
         (when-let [bot (:braid/ok reply)]
           (on-complete bot))))}))

(reg-event-fx
  :create-upload
  (fn [{state :db :as cofx} [_ {:keys [url thread-id group-id]}]]
    {:websocket-send (list [:braid.server/create-upload
                            (schema/make-upload {:url url :thread-id thread-id})])
     :dispatch [:new-message {:content url :thread-id thread-id :group-id group-id}]}))

(reg-event-fx
  :get-group-uploads
  (fn [cofx [_ {:keys [group-id on-success on-error]}]]
    {:websocket-send
     (list
       [:braid.server/uploads-in-group group-id]
       5000
       (fn [reply]
         (if-let [uploads (:braid/ok reply)]
           (on-success uploads)
           (on-error (get reply :braid/error "Couldn't get uploads in group")))))}))

(reg-event-fx
  :check-auth
  (fn [cofx _]
    {:edn-xhr {:uri "/check"
               :method :get
               :on-complete (fn [_] (dispatch [:start-socket]))
               :on-error (fn [_] (dispatch [:set-login-state :login-form]))}}))

(reg-event-db
  :set-login-state
  (fn [state [_ login-state]]
    (helpers/set-login-state state login-state)))

(reg-event-db
  :start-socket
  (fn [state [_ _]]
    ; TODO
    (sync/make-socket!)
    ; TODO
    (sync/start-router!)
    (helpers/set-login-state state :ws-connect)))

(reg-event-fx
  :set-window-visibility
  (fn [{state :db :as cofx} [_ visible?]]
    (merge
      {:db (helpers/set-window-visibility state visible?)}
      (when visible?
        {:window-title "Chat"}))))

(reg-event-fx
  :auth
  (fn [cofx [_ data]]
    {:edn-xhr {:uri "/auth"
               :method :post
               :params {:email (data :email)
                        :password (data :password)}
               :on-complete (fn [_]
                              (when-let [cb (data :on-complete)]
                                (cb))
                              (dispatch [:start-socket]))
               :on-error (fn [_]
                           (when-let [cb (data :on-error)]
                             (cb)))}}))

(reg-event-fx
  :request-reset
  (fn [cofx [_ email]]
    {:edn-xhr {:uri "/request-reset"
               :method :post
               :params {:email email}}}))

(reg-event-fx
  :logout
  (fn [cofx [_ _]]
    {:edn-xhr {:uri "/logout"
               :method :post
               :params {:csrf-token (:csrf-token @sync/chsk-state)}
               :on-complete (fn [data]
                              (dispatch [:set-login-state :login-form])
                              (dispatch [:clear-session]))}}))

(reg-event-db
  :set-group-and-page
  (fn [state [_ [group-id page-id]]]
    (helpers/set-group-and-page state group-id page-id)))

(reg-event-db
  :set-page-loading
  (fn [state [_ bool]]
    (helpers/set-page-loading state bool)))

(reg-event-db
  :set-page-error
  (fn [state [_ bool]]
    (helpers/set-page-error state bool)))

(reg-event-fx
  :set-preference
  (fn [{state :db :as cofx} [_ [k v]]]
    {:websocket-send (list [:braid.server/set-preferences {k v}])
     :db (helpers/set-preferences state {k v})}))

(reg-event-db
  :add-users
  (fn [state [_ users]]
    (helpers/add-users state users)))

(reg-event-db
  :join-group
  (fn [state [_ {:keys [group tags]}]]
    (let [state (-> state
                    (helpers/add-tags tags)
                    (helpers/add-group group))])
    (reduce (fn [s tag]
              (helpers/subscribe-to-tag s (tag :id)))
            state
            tags)))

(reg-event-fx
  :notify-if-client-out-of-date
  (fn [cofx [_ server-checksum]]
    (if (not= (aget js/window "checksum") server-checksum)
      {:dispatch [:display-error
                  [:client-out-of-date "Client out of date - please refresh" :info]]}
      {})))

(reg-event-db
  :set-init-data
  (fn [state [_ data]]
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
        (helpers/set-open-threads (data :user-threads))
        (quest-helpers/set-quest-records (data :quest-records)))))

(reg-event-fx
  :leave-group
  (fn [{state :db :as cofx} [_ {:keys [group-id group-name]}]]
    (merge
      {:db
       (-> state
           (helpers/display-error (str "left-" group-id)
                                  (str "You have been removed from " group-name)
                                  :info)
           (helpers/remove-group group-id)
           ((fn [state]
              (if-let [sidebar-order (:groups-order
                                       (helpers/get-user-preferences state))]
                (helpers/set-preferences
                  state
                  {:groups-order (into [] (remove (partial = group-id))
                                       sidebar-order)})
                state))))}
      (when (= group-id (helpers/get-open-group-id state))
        {:redirect-to "/"}))))

(reg-event-db
  :add-open-thread
  (fn [state [_ thread]]
    (helpers/add-open-thread state thread)))

(reg-event-fx
  :maybe-increment-unread
  (fn [{state :db :as cofx} _]
    (when-not (get-in state [:notifications :window-visible?])
      (let [state (update-in state [:notifications :unread-count] inc)
            unread (get-in state [:notifications :unread-count])]
        {:db state
         :window-title (str "Chat (" unread ")")}))))

(reg-event-db
  :add-invite
  (fn [state [_ invite]]
    (helpers/add-invite state invite)))

(reg-event-db
  :update-user-status
  (fn [state [_ [user-id status]]]
    (helpers/update-user-status state user-id status)))

(reg-event-db
  :add-user
  (fn [state [_ user]]
    (helpers/add-user state user)))

(reg-event-db
  :remove-user-from-group
  (fn [state [_ [group-id user-id]]]
    (helpers/remove-user-from-group state user-id group-id)))

(reg-event-db
  :set-group-publicity
  (fn [state [_ [group-id publicity]]]
    (helpers/set-group-publicity state group-id publicity)))

(reg-event-db
  :add-group-bot
  (fn [state [_ [group-id bot]]]
    (helpers/add-group-bot state group-id bot)))
