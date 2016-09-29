(ns braid.client.core.events
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [braid.client.store :as store]
            [braid.client.sync :as sync]
            [braid.client.schema :as schema]
            [braid.common.util :as util]
            [braid.client.router :as router]
            [braid.client.routes :as routes]
            [braid.client.xhr :refer [edn-xhr]]
            [braid.client.state.helpers :as helpers :refer [key-by-id]]
            [braid.client.quests.helpers :as quest-helpers]))

; TODO: handle callbacks declaratively too?
(reg-fx :websocket-send (fn [args] (when args (apply sync/chsk-send! args))))

; TODO: handle callbacks declaratively too?
(reg-fx :edn-xhr (fn [args] (edn-xhr args)))

(reg-fx :redirect-to (fn [route] (when route (router/go-to route))))

(reg-fx :window-title (fn [title] (when title (set! (.-title js/document) title))))

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
    (assoc-in state [:session] nil)))

(reg-event-db
  :new-message-text
  (fn [state [_ {:keys [group-id thread-id content]}]]
    (if (get-in state [:threads thread-id])
      (assoc-in state [:threads thread-id :new-message] content)
      (assoc-in state [:temp-threads group-id :new-message] content))))

(reg-event-db
  :display-error
  (fn [state [_ args]]
    (apply helpers/display-error state args)))

(reg-event-db
  :set-message-failed
  (fn [state [_ message]]
    (update-in state [:threads (message :thread-id) :messages]
               (partial map (fn [msg] (if (= (message :id) (msg :id))
                                        (assoc msg :failed? true)
                                        msg))))))

(reg-event-fx
  :new-message
  (fn [{state :db :as cofx} [_ data]]
    (when-not (string/blank? (data :content))
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
               (dispatch [:display-error [(str :failed-to-send (message :id))
                                          "Message failed to send!"]])
               (dispatch [:set-message-failed message]))))
         :db (-> state
                 (helpers/add-message message)
                 (helpers/maybe-reset-temp-thread (data :thread-id)))}))))

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
             (update-in [:threads (message :thread-id) :messages]
                        (partial map (fn [msg]
                                       (if (= (message :id) (msg :id))
                                         (dissoc msg :failed?)
                                         msg)))))}))

(reg-event-fx
  :hide-thread
  (fn [{state :db :as cofx} [_ {:keys [thread-id local-only?]}]]
    {:db (update-in state [:user :open-thread-ids] disj thread-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/hide-thread thread-id]))}))

(reg-event-fx
  :reopen-thread
  (fn [{state :db :as cofx} [_ thread-id]]
    {:websocket-send (list [:braid.server/show-thread thread-id])
     :db (update-in state [:user :open-thread-ids] conj  thread-id)}))

(reg-event-fx
  :unsub-thread
  (fn [cofx [_ data]]
    {:websocket-send (list [:braid.server/unsub-thread (data :thread-id)])
     :dispatch [:hide-thread {:thread-id (data :thread-id) :local-only? true}]}))

(reg-event-fx
  :create-tag
  (fn [{state :db :as cofx} [_ {:keys [tag local-only?]}]]
    (let [tag (merge (schema/make-tag) tag)]
      {:db (-> state
               (assoc-in [:tags (tag :id)] tag)
               (helpers/subscribe-to-tag (tag :id)))
       :websocket-send
       (when-not local-only?
         (list
           [:braid.server/create-tag tag]
           1000
           (fn [reply]
             (if-let [msg (:error reply)]
               (do
                 (dispatch [:remove-tag {:tag-id (tag :id) :local-only? true}])
                 (dispatch [:display-error [(str :bad-tag (tag :id)) msg]]))))))})))

(reg-event-fx
  :unsubscribe-from-tag
  (fn [{state :db :as cofx} [_ tag-id]]
    {:websocket-send (list [:braid.server/unsubscribe-from-tag tag-id])
     :db (update-in state [:user :subscribed-tag-ids] disj tag-id)}))

(reg-event-fx
  :subscribe-to-tag
  (fn [{state :db :as cofx} [_ {:keys [tag-id local-only?]}]]
    {:db (helpers/subscribe-to-tag state tag-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/subscribe-to-tag tag-id]))}))

(reg-event-fx
  :set-tag-description
  (fn [{state :db :as cofx} [_ {:keys [tag-id description local-only?]}]]
    {:db (assoc-in state [:tags tag-id :description] description)
     :websocket-send
     (when-not local-only?
       (list
         [:braid.server/set-tag-description {:tag-id tag-id
                                             :description description}]))}))

(reg-event-fx
  :remove-tag
  (fn [{state :db :as cofx} [_ {:keys [tag-id local-only?]}]]
    {:db
     (-> state
         (update :threads
                 (partial into {}
                          (map (fn [[t-id t]]
                                 [t-id
                                  (update t :tag-ids disj tag-id)]))))
         (update :tags dissoc tag-id))
     :websocket-send (when-not local-only?
                       (list [:braid.server/retract-tag tag-id]))}))

(reg-event-db
  :remove-group
  (fn [state [_ group-id]]
    (helpers/remove-group state group-id)))

(reg-event-fx
  :create-group
  (fn [{state :db :as cofx} [_ group]]
    (let [group (schema/make-group group)
          group-id (group :id)
          user-id (helpers/current-user-id state)]
      {:websocket-send
       (list
         [:braid.server/create-group group]
         1000
         (fn [reply]
           (when-let [msg (reply :error)]
             (.error js/console msg)
             (dispatch [:display-error [(str :bad-group group-id) msg]])
             (dispatch [:remove-group group-id]))))
       :db (-> state
               (helpers/add-group group)
               (update-in [:users user-id :group-ids]
                          #(vec (conj (set %) group-id)))
               (update-in [:groups group-id :admins] conj user-id))})))

(reg-event-db
  :update-user-nickname
  (fn [state [_ {:keys [nickname user-id]}]]
    (assoc-in state [:users user-id :nickname] nickname)))

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

; TODO: merge set-user-avatar & update-user-avatar?
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
  (fn [state [_ [query {:keys [threads thread-ids] :as reply}]]]
    (-> state
        (update-in [:threads] #(merge-with merge % (key-by-id threads)))
        (update-in [:page] (fn [p] (if (= (p :search-query) query)
                                     (assoc p :thread-ids thread-ids)
                                     p))))))

(reg-event-db
  :set-search-query
  (fn [state [_ query]]
    (assoc-in state [:page :search-query] query)))

(reg-event-fx
  :search-history
  (fn [{state :db :as cofx} [_ [query group-id]]]
    (when query
      {:websocket-send
       (list
         [:braid.server/search [query group-id]]
         15000
         (fn [reply]
           (dispatch [:set-page-loading false])
           (if (:thread-ids reply)
             (dispatch [:set-search-results [query reply]])
             (dispatch [:set-page-error true]))))
       :dispatch [:set-page-error false]})))

(reg-event-db
  :add-threads
  (fn [state [_ threads]]
    (-> state
        (update :threads #(merge-with merge % (key-by-id threads)))
        (update :group-threads
                #(merge-with
                   set/union
                   %
                   (into {}
                         (map (fn [[g t]] [g (into #{} (map :id) t)]))
                         (group-by :group-id threads)))))))

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

(reg-event-fx
  :mark-thread-read
  (fn [{state :db :as cofx} [_ thread-id]]
    {:websocket-send (list [:braid.server/mark-thread-read thread-id])
     :db (helpers/update-thread-last-open-at state thread-id)}))

(reg-event-db
  :focus-thread
  (fn [state [_ thread-id]]
    (assoc-in state [:focused-thread-id] thread-id)))

(reg-event-fx
  :clear-inbox
  (fn [{state :db :as cofx} [_ _]]
    {:dispatch-n
     (into ()
           (comp
             (filter (fn [thread] (= (state :open-group-id) (thread :group-id))))
             (map :id)
             (map (fn [id] [:hide-thread {:thread-id id :local-only? false}])))
           (-> (state :threads)
               (select-keys (get-in state [:user :open-thread-ids]))
               vals))}))

(reg-event-fx
  :make-admin
  (fn [{state :db :as cofx} [_ {:keys [group-id user-id local-only?] :as args}]]
    {:db (update-in state [:groups group-id :admins] conj user-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/make-user-admin args]))}))

(reg-event-fx
  :remove-from-group
  (fn [cofx [ _ {:keys [group-id user-id] :as args}]]
    {:websocket-send (list [:braid.server/remove-from-group args])
     :dispatch [:redirect-to-first-group]}))

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
    (assoc state :login-state login-state)))

(reg-event-fx
  :start-socket
  (fn [cofx [_ _]]
    ; TODO
    (sync/make-socket!)
    ; TODO
    (sync/start-router!)
    {:dispatch [:set-login-state :ws-connect]}))

(reg-event-fx
  :set-window-visibility
  (fn [{state :db :as cofx} [_ visible?]]
    {:db (-> state
             (assoc-in [:notifications :window-visible?] visible?)
             (update-in [:notifications :unread-count]
                        (if visible? (constantly 0) identity)))
     :window-title (when visible? "Braid")}))

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

(reg-event-fx
  :set-group-and-page
  (fn [{state :db :as cofx} [_ [group-id page-id]]]
    (if (or (nil? group-id) (some? (get-in state [:groups group-id])))
      {:db (assoc state :open-group-id group-id :page page-id)}
      {:dispatch [:redirect-to-first-group]})))

(reg-event-fx
  :redirect-to-first-group
  (fn [{state :db :as cofx} _]
    {:redirect-to
     (when-let [group-id (-> (helpers/ordered-groups state)
                             first
                             :id)]
       (routes/inbox-page-path {:group-id group-id}))}))

(reg-event-db
  :set-page-loading
  (fn [state [_ bool]]
    (assoc-in state [:page :loading?] bool)))

(reg-event-db
  :set-page-error
  (fn [state [_ bool]]
    (assoc-in state [:page :error?] bool)))

(reg-event-fx
  :set-preference
  (fn [{state :db :as cofx} [_ [k v]]]
    {:websocket-send (list [:braid.server/set-preferences {k v}])
     :db (helpers/set-preferences state {k v})}))

(reg-event-db
  :add-users
  (fn [state [_ users]]
    (update-in state [:users] merge (key-by-id users))))

(reg-event-db
  :join-group
  (fn [state [_ {:keys [group tags]}]]
    (-> state
        (helpers/add-tags tags)
        (helpers/add-group group)
        (update-in [:user :subscribed-tag-ids]
                   set/union (set (map :id tags))))))

(reg-event-fx
  :notify-if-client-out-of-date
  (fn [cofx [_ server-checksum]]
    (if (not= (aget js/window "checksum") server-checksum)
      {:dispatch [:display-error
                  [:client-out-of-date "Client out of date - please refresh" :info]]}
      {})))

(reg-event-fx
  :set-init-data
  (fn [{state :db :as cofx} [_ data]]
    {:dispatch-n (list [:set-login-state :app]
                   [:add-users (data :users)])
     :db (-> state
             (assoc :session {:user-id (data :user-id)})
             (assoc-in [:user :subscribed-tag-ids]
               (set (data :user-subscribed-tag-ids)))
             (assoc :groups (key-by-id (data :user-groups)))
             (assoc :invitations (data :invitations))
             (assoc :threads (key-by-id (data :user-threads)))
             (assoc :group-threads
               (into {}
                     (map (fn [[g t]] [g (into #{} (map :id) t)]))
                     (group-by :group-id (data :user-threads))))
             (assoc-in [:user :open-thread-ids]
               (set (map :id (data :user-threads))))
             (assoc :temp-threads (->> (data :user-groups)
                                       (map :id)
                                       (reduce (fn [memo group-id]
                                                 (assoc memo group-id (schema/make-temp-thread group-id))) {})))
             (helpers/add-tags (data :tags))
             (helpers/set-preferences (data :user-preferences))
             (quest-helpers/set-quest-records (data :quest-records)))}))

(reg-event-fx
  :leave-group
  (fn [{state :db :as cofx} [_ {:keys [group-id group-name]}]]
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
              state))))
     :dispatch [(when (= group-id (state :open-group-id))
                 :redirect-to-first-group)]}))

(reg-event-db
  :add-open-thread
  (fn [state [_ thread]]
    (-> state
        (update-in [:threads (thread :id)] merge thread)
        (update-in [:group-threads (thread :group-id)]
                   #(conj (set %) (thread :id)))
        (update-in [:user :open-thread-ids] conj (thread :id)))))

(reg-event-fx
  :maybe-increment-unread
  (fn [{state :db :as cofx} _]
    (when-not (get-in state [:notifications :window-visible?])
      (let [state (update-in state [:notifications :unread-count] inc)
            unread (get-in state [:notifications :unread-count])]
        {:db state
         :window-title (str "Braid (" unread ")")}))))

(reg-event-db
  :update-user-status
  (fn [state [_ [user-id status]]]
    (if (get-in state [:users user-id])
      (assoc-in state [:users user-id :status] status)
      state)))

(reg-event-db
  :add-user
  (fn [state [_ user]]
    (update-in state [:users] assoc (:id user) user)))

(reg-event-db
  :remove-user-from-group
  (fn [state [_ [group-id user-id]]]
    ; TODO: also remove user from collection if group-ids is now empty?
    ; shouldn't make a difference
    ; TODO: remove mentions of that user from the group?
    (update-in state [:users user-id :group-ids]
               (partial remove (partial = group-id)))))

(reg-event-fx :add-tag-to-thread
  (fn [{state :db :as cofx} [_ {:keys [thread-id tag-id local-only?]}]]
    (if (get-in state [:threads thread-id])
      {:db (update-in state [:threads thread-id :tag-ids] conj tag-id)
       :websocket-send (when-not local-only?
                         (list
                           [:braid.server/tag-thread {:thread-id thread-id
                                                      :tag-id tag-id}]))}
      {:db (update-in state [:temp-threads (state :open-group-id) :tag-ids]
                      conj tag-id)})))

(reg-event-fx
  :go-to
  (fn [_ [_ route]]
    {:redirect-to route}))
