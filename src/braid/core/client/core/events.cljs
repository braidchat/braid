(ns braid.core.client.core.events
  (:require
   [braid.core.client.desktop.notify :as notify]
   [braid.core.client.router :as router]
   [braid.core.client.routes :as routes]
   [braid.core.client.schema :as schema]
   [braid.core.client.state :refer [reg-event-fx]]
   [braid.core.client.state.helpers :as helpers :refer [key-by-id]]
   [braid.core.client.sync :as sync]
   [braid.core.client.xhr :refer [edn-xhr]]
   [braid.core.common.util :as util]
   [braid.core.hooks :as hooks]
   [clojure.set :as set]
   [clojure.string :as string]
   [re-frame.core :as re-frame :refer [dispatch reg-fx]]))

(defonce event-listeners
  (hooks/register! (atom []) [fn?]))

(re-frame/add-post-event-callback
  (fn [event _]
    (doseq [handler @event-listeners]
      (handler event))))

(defn register-events!
  [event-map]
  (doseq [[k f] event-map]
    (reg-event-fx k f)))

; TODO: handle callbacks declaratively too?
(reg-fx :websocket-send (fn [args] (when args (apply sync/chsk-send! args))))

; TODO: handle callbacks declaratively too?
(reg-fx :edn-xhr (fn [args] (edn-xhr args)))

(reg-fx :redirect-to (fn [route] (when route (router/go-to route))))

(reg-fx :window-title (fn [title] (when title (set! (.-title js/document) title))))

(reg-fx :notify notify/notify)

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
  (vals (get-in state [:groups (:open-group-id state) :users])))

(defn user-in-open-group?
  [state user-id]
  (get-in state [:groups (:open-group-id state) :users user-id]))

(defn extract-user-ids
  [state text]
  (let [mentioned-names (->> (re-seq util/sigiled-nickname-re text)
                             (map second))
        nick->id (reduce (fn [m {:keys [id nickname]}] (assoc m nickname id))
                         {}
                         (all-users state))]
    (->> mentioned-names
         (map nick->id)
         (remove nil?))))

(defn nickname->user
  [state nickname]
  (->> (get-in state [:groups (:open-group-id state) :users])
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


(reg-event-fx
  :initialize-db
  (fn [{db :db} _]
    {:db (braid.core.client.state/initialize-state db)
     :dispatch [:braid.core.client.gateway.events/initialize :log-in]}))

(reg-event-fx
  :new-message-text
  (fn [{db :db} [_ {:keys [group-id thread-id content]}]]
    {:db (cond
           (get-in db [:threads thread-id])
           (assoc-in db [:threads thread-id :new-message] content)

           (get-in db [:temp-threads group-id])
           (assoc-in db [:temp-threads group-id :new-message] content)

           :else db)}))

(reg-event-fx
  :set-message-failed
  (fn [{db :db} [_ message]]
    {:db (update-in db [:threads (message :thread-id) :messages]
                    (partial map (fn [msg] (if (= (message :id) (msg :id))
                                             (assoc msg :failed? true)
                                             msg))))}))

(reg-event-fx
  :new-message
  (fn [{state :db} [_ data]]
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
               (dispatch [:braid.notices/display! [(keyword "failed-to-send" (message :id))
                                                   "Message failed to send!"
                                                   :error]])
               (dispatch [:set-message-failed message]))))
         :db (-> state
                 (helpers/add-message message)
                 (helpers/maybe-reset-temp-thread (data :thread-id)))}))))

(reg-event-fx
  :core/retract-message
  (fn [{db :db} [_ {:keys [thread-id message-id remote?]}]]
    (when (get-in db [:threads thread-id])
      (cond-> {:db (update-in
                     db [:threads thread-id :messages]
                     (partial
                       into [] (remove (fn [{id :id}] (= id message-id)))))}
        remote? (assoc :websocket-send
                       (list [:braid.server/retract-message message-id]))))))

(reg-event-fx
  :resend-message
  (fn [{state :db} [_ message]]
    {:websocket-send (list
                       [:braid.server/new-message message]
                       2000
                       (fn [reply]
                         (when (not= :braid/ok reply)
                           (dispatch [:braid.notices/display!
                                      [(keyword "failed-to-send" (message :id))
                                       "Message failed to send!"
                                       :error]])
                           (dispatch [:set-message-failed message]))))

     :dispatch [:braid.notices/clear! (keyword "failed-to-send" (message :id))]
     :db (-> state
             (update-in [:threads (message :thread-id) :messages]
                        (partial map (fn [msg]
                                       (if (= (message :id) (msg :id))
                                         (dissoc msg :failed?)
                                         msg)))))}))

(reg-event-fx
  :hide-thread
  (fn [{state :db} [_ {:keys [thread-id local-only?]}]]
    {:db (update-in state [:user :open-thread-ids] disj thread-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/hide-thread thread-id]))}))

(reg-event-fx
  :reopen-thread
  (fn [{state :db} [_ thread-id]]
    {:websocket-send (list [:braid.server/show-thread thread-id])
     :db (update-in state [:user :open-thread-ids] conj  thread-id)}))

(reg-event-fx
  :unsub-thread
  (fn [_ [_ data]]
    {:websocket-send (list [:braid.server/unsub-thread (data :thread-id)])
     :dispatch [:hide-thread {:thread-id (data :thread-id) :local-only? true}]}))

(reg-event-fx
  :create-tag
  (fn [{state :db} [_ {:keys [tag local-only?]}]]
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
             (when-let [msg (:error reply)]
               (do
                 (dispatch [:remove-tag {:tag-id (tag :id) :local-only? true}])
                 (dispatch [:braid.notices/display! [(keyword "bad-tag" (tag :id)) msg :error]]))))))})))

(reg-event-fx
  :unsubscribe-from-tag
  (fn [{state :db} [_ tag-id]]
    {:websocket-send (list [:braid.server/unsubscribe-from-tag tag-id])
     :db (update-in state [:user :subscribed-tag-ids] disj tag-id)}))

(reg-event-fx
  :subscribe-to-tag
  (fn [{state :db} [_ {:keys [tag-id local-only?]}]]
    {:db (helpers/subscribe-to-tag state tag-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/subscribe-to-tag tag-id]))}))

(reg-event-fx
  :set-tag-description
  (fn [{state :db} [_ {:keys [tag-id description local-only?]}]]
    {:db (assoc-in state [:tags tag-id :description] description)
     :websocket-send
     (when-not local-only?
       (list
         [:braid.server/set-tag-description {:tag-id tag-id
                                             :description description}]))}))

(reg-event-fx
  :remove-tag
  (fn [{state :db} [_ {:keys [tag-id local-only?]}]]
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

(reg-event-fx
  :update-user-nickname
  (fn [{db :db} [_ {:keys [nickname user-id group-ids]}]]
    {:db (reduce (fn [state group-id]
                   (assoc-in state [:groups group-id :users user-id :nickname] nickname))
                 db group-ids)}))

(reg-event-fx
  :set-user-nickname
  (fn [{state :db} [_ {:keys [nickname on-error]}]]
    {:websocket-send
     (list
       [:braid.server/set-nickname {:nickname nickname}]
       1000
       (fn [reply]
         (if-let [msg (reply :error)]
           (on-error msg)
           (dispatch [:update-user-nickname
                      {:nickname nickname
                       :user-id (helpers/current-user-id state)
                       :group-ids (keys (state :groups))}]))))}))

(reg-event-fx
  :set-user-avatar
  (fn [{state :db} [_ avatar-url]]
    {:websocket-send (list [:braid.server/set-user-avatar avatar-url])}))

(reg-event-fx
  :update-user-avatar
  (fn [{db :db} [_ {:keys [user-id group-id avatar-url]}]]
    {:db (helpers/update-user-avatar db {:group-id group-id
                                         :user-id user-id
                                         :avatar-url avatar-url})}))

(reg-event-fx
  :set-password
  (fn [_ [_ [password on-success on-error]]]
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
  (fn [{state :db} [_ rule]]
    (let [current-rules (get (helpers/get-user-preferences state)
                          :notification-rules [])]
      {:dispatch [:set-preference [:notification-rules (conj current-rules rule)]]})))

(reg-event-fx
  :remove-notification-rule
  (fn [{state :db} [_ rule]]
    (let [new-rules (into [] (remove (partial = rule))
                          (get (helpers/get-user-preferences state)
                            :notification-rules []))]
      {:dispatch [:set-preference [:notification-rules new-rules]]})))

(reg-event-fx
  :set-search-results
  (fn [{db :db} [_ [query {:keys [threads thread-ids] :as reply}]]]
    {:db (-> db
             (update-in [:threads] #(merge-with merge % (key-by-id threads)))
             (update-in [:page] (fn [p] (if (= (p :query) query)
                                          (assoc p :thread-ids thread-ids)
                                          p))))}))

(reg-event-fx
  :set-search-query
  (fn [{db :db} [_ query]]
    {:db (assoc-in db [:page :query] query)}))

(reg-event-fx
  :search-history
  (fn [{state :db} [_ [query group-id]]]
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

(reg-event-fx
  :add-threads
  (fn [{db :db} [_ threads ?open]]
    {:db (-> db
             (update :threads #(merge-with merge % (key-by-id threads)))
             (update :group-threads
                     #(merge-with
                        set/union
                        %
                        (into {}
                              (map (fn [[g t]] [g (into #{} (map :id) t)]))
                              (group-by :group-id threads))))
             (cond->
               ?open (update-in [:user :open-thread-ids] into
                                (map :id threads))))}))

(reg-event-fx
  :load-recent-threads
  (fn [_ [_ {:keys [group-id on-error on-complete]}]]
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
  (fn [_ [_ {:keys [thread-ids on-complete]}]]
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
  (fn [{state :db} [_ thread-id]]
    {:websocket-send (list [:braid.server/mark-thread-read thread-id])
     :db (helpers/update-thread-last-open-at state thread-id)}))

(reg-event-fx
  :focus-thread
  (fn [{db :db} [_ thread-id]]
    {:db (assoc-in db [:focused-thread-id] thread-id)}))

(reg-event-fx
  :clear-inbox
  (fn [{state :db} [_ _]]
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
  (fn [{state :db} [_ {:keys [group-id user-id local-only?] :as args}]]
    {:db (update-in state [:groups group-id :admins] conj user-id)
     :websocket-send (when-not local-only?
                       (list [:braid.server/make-user-admin args]))}))

(reg-event-fx
  :remove-from-group
  (fn [_ [ _ {:keys [group-id user-id] :as args}]]
    {:websocket-send (list [:braid.server/remove-from-group args])
     :dispatch [:redirect-from-root]}))

(reg-event-fx
  :set-login-state
  (fn [{db :db} [_ login-state]]
    {:db (assoc db :login-state login-state)}))

(reg-event-fx
  :start-anon-socket
  (fn [_ [_ _]]
    (sync/connect!)
    {}))

(reg-event-fx
  :start-socket
  (fn [_ [_ _]]
    (sync/connect!)
    {:dispatch [:set-login-state :ws-connect]}))

(reg-event-fx
  :set-window-visibility
  (fn [{state :db} [_ visible?]]
    {:db (-> state
             (assoc-in [:notifications :window-visible?] visible?)
             (update-in [:notifications :unread-count]
                        (if visible? (constantly 0) identity)))
     :window-title (when visible? "Braid")}))

(reg-event-fx
  :auth
  (fn [_ [_ data]]
    {:edn-xhr {:uri "/session"
               :method :put
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
  (fn [_ [_ email]]
    {:edn-xhr {:uri "/request-reset"
               :method :post
               :params {:email email}}}))

(reg-event-fx
  :logout
  (fn [_ [_ _]]
    {:edn-xhr {:uri "/session"
               :method :delete
               :headers {"x-csrf-token" (:csrf-token @sync/chsk-state)}
               :on-complete (fn [data]
                              (sync/disconnect!)
                              (dispatch [:initialize-db])
                              (dispatch [:set-login-state :gateway])
                              (dispatch [:go-to "/"]))}}))

(reg-event-fx
  :set-group-and-page
  (fn [{state :db} [_ [group-id page-id]]]
    (cond
      (and (= :gateway (:login-state state)) group-id)
      {:dispatch [:braid.core.client.gateway.forms.user-auth.events/load-group-readonly
                  group-id]}

      (nil? group-id)
      {:db (assoc state :open-group-id nil :page page-id)}

      (get-in state [:groups group-id :readonly])
      {:db (assoc state :open-group-id group-id :page {:type :readonly})}

      (some? (get-in state [:groups group-id]))
      {:db (assoc state :open-group-id group-id :page page-id)}

      :else
      {:dispatch [:core/load-readonly-group group-id]})))

(reg-event-fx
  :redirect-from-root
  (fn [{state :db} _]
    (if (state :session)
      {:redirect-to
       (if-let [group-id (-> (helpers/ordered-groups state)
                             first
                             :id)]
         (routes/group-page-path {:group-id group-id
                                  :page-id "inbox"})
         (routes/system-page-path {:page-id "group-explore"}))}
      {})))

(reg-event-fx
  :set-page-loading
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:page :loading?] bool)}))

(reg-event-fx
  :set-page-error
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:page :error?] bool)}))

(reg-event-fx
  :set-preference
  (fn [{state :db} [_ [k v]]]
    {:websocket-send (list [:braid.server/set-preferences {k v}])
     :db (helpers/set-preferences state {k v})}))

(reg-event-fx
  :add-users
  (fn [{db :db} [_ [group-id users]]]
    {:db (update-in db [:groups group-id :users] merge (key-by-id users))}))

(reg-event-fx
  :join-group
  (fn [{state :db} [_ {:keys [group tags]}]]
    (-> {:db (-> state
                 (helpers/add-tags tags)
                 (helpers/add-group group)
                 (update-in [:user :subscribed-tag-ids]
                            set/union (set (map :id tags))))}
       (cond->
           (and (= (:id group) (:open-group-id state))
                (= :readonly (get-in state [:page :type])))
         (assoc :redirect-to (routes/group-page-path {:group-id (:id group)
                                                      :page-id "inbox"}))))))

(reg-event-fx
  :notify-if-client-out-of-date
  (fn [_ [_ server-checksum]]
    (if (not= (aget js/window "checksum") server-checksum)
      {:dispatch [:braid.notices/display!
                  [:client-out-of-date "Client out of date - please refresh" :info]]}
      {})))

(defonce initial-user-data-handlers
  (hooks/register! (atom []) [fn?]))

(reg-event-fx
  :set-init-data
  (fn [{state :db} [_ data]]
    {:dispatch-n (list [:set-login-state :app])
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
                                                (->> (schema/make-temp-thread group-id)
                                                    (assoc memo group-id)))
                                              {})))
             (helpers/add-tags (data :tags))
             (helpers/set-preferences (data :user-preferences))
             (as-> <>
                 (reduce (fn [db f] (f db data))
                         <>
                         @initial-user-data-handlers)))}))

(reg-event-fx
  :leave-group
  (fn [{state :db} [_ {:keys [group-id group-name]}]]
    {:dispatch-n
     [[:braid.notices/display!
       [(keyword "left-group" group-id)
        (str "You have been removed from " group-name)
        :info]]
      (when (= group-id (state :open-group-id))
        [:redirect-from-root])]
     :db
     (-> state
         (helpers/remove-group group-id)
         (cond->
           (get-in state [:preferences :groups-order])
           (update-in [:preferences :groups-order]
                      (partial filterv #(not= % group-id)))))}))

(reg-event-fx
  :add-open-thread
  (fn [{state :db} [_ thread]]
    (let [msg-ids (map (comp (partial str :failed-to-send) :id)
                       (:messages thread))]
      {:dispatch [:braid.notices/clear! msg-ids]
       :db
       (-> state
           (update-in [:threads (thread :id)] merge thread)
           (update-in [:group-threads (thread :group-id)]
                      #(conj (set %) (thread :id)))
           (update-in [:user :open-thread-ids] conj (thread :id)))})))

(reg-event-fx
  :maybe-increment-unread
  (fn [{state :db} _]
    (when-not (get-in state [:notifications :window-visible?])
      (let [state (update-in state [:notifications :unread-count] inc)
            unread (get-in state [:notifications :unread-count])]
        {:db state
         :window-title (str "Braid (" unread ")")}))))

(reg-event-fx
  :update-user-status
  (fn [{db :db} [_ [group-id user-id status]]]
    {:db (if (get-in db [:groups group-id :users user-id])
           (assoc-in db [:groups group-id :users user-id :status] status)
           db)}))

(reg-event-fx
  :add-user
  (fn [{db :db} [_ group-id user]]
    {:db (assoc-in db [:groups group-id :users (:id user)] user)}))

(reg-event-fx
  :remove-user-from-group
  (fn [{db :db} [_ [group-id user-id]]]
    ;; TODO: remove mentions of that user from the group?
    {:db (if (get-in db [:groups group-id :users])
           (update-in db [:groups group-id :users] dissoc user-id)
           db)}))

(reg-event-fx :add-tag-to-thread
  (fn [{state :db} [_ {:keys [thread-id tag-id local-only?]}]]
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

(reg-event-fx
  :core/websocket-disconnected
  (fn [{state :db} _]
    {:db (assoc-in state [:websocket-state :connected?] false)}))

(reg-event-fx
  :core/websocket-connected
  (fn [{state :db} _]
    {:db (assoc-in state [:websocket-state :connected?] true)}))

(reg-event-fx
  :core/websocket-needs-auth
  (fn [{state :db} _]
    {:dispatch-n [[:initialize-db]
                  [:set-login-state :gateway]]}))

(reg-event-fx
  :core/websocket-anon-connected
  (fn [{db :db} _]
    (if (= :anon-ws-connect (:login-state db))
      {:dispatch-n [[:set-login-state :anon-connected]
                    [:core/load-readonly-group]]}
      {:dispatch-n [[:core/websocket-needs-auth]]})))

(reg-event-fx
  :core/load-readonly-group
  (fn [{db :db} [_ ?group-id]]
    {:websocket-send
     (list [:braid.server.anon/load-group (or ?group-id (:open-group-id db))]
           5000
           (fn [resp]
             (case resp
               :chsk/timeout (js/console.warn "Loading readonly group timed out")
               :braid/error (do (prn "private or nonexistant group")
                                ;; should probably show warning here?
                                (dispatch [:redirect-from-root]))

               (do (dispatch [:join-group (select-keys resp [:tags :group])])
                   (dispatch [:add-threads (:threads resp) true])
                   (when ?group-id
                     (dispatch [:set-group-and-page [?group-id {:type :readonly}]]))))))}))

(reg-event-fx
  :core/join-public-group
  (fn [{db :db} [_ group-id]]
    (if (get-in db [:session :user-id])
      {:websocket-send
       (list [:braid.server/join-public-group group-id])}
      {:redirect-to (routes/join-group-path {:group-id group-id})})))

(reg-event-fx
  :core/websocket-update-next-reconnect
  (fn [{state :db} [_ next-reconnect]]
    {:db (assoc-in state [:websocket-state :next-reconnect] next-reconnect)}))

(reg-event-fx
  :core/show-message-notification
  (fn [{db :db} [_ {:keys [content group-id user-id] :as msg}]]
    (let [sender (get-in db [:groups group-id :users user-id])]
      {:notify {:body (str (sender :nickname) ": " content)
                :title (str "New Message in "
                            (get-in db [:groups group-id :name]))
                :icon (sender :avatar)}})))

(reg-event-fx
  :core/reconnect
  (fn [_ _]
    (sync/reconnect!)
    {}))
