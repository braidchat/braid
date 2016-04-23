(ns chat.client.store
  (:require [cljs-utils.core :refer [flip]]
            [cljs-uuid-utils.core :as uuid]
            [reagent.core :as r]))

(defonce app-state
  (r/atom
    {:login-state :auth-check ; :ws-connect :login-form :app
     :open-group-id nil
     :threads {}
     :pagination-remaining 0
     :users {}
     :tags {}
     :groups {}
     :page {:type :inbox}
     :session nil
     :errors []
     :invitations []
     :preferences {}
     :notifications {:window-visible? true
                     :unread-count 0}
     :user {:open-thread-ids #{}
            :subscribed-tag-ids #{}
            :user-id nil
            :nickname nil}
     :new-thread-id (uuid/make-random-squuid)}))

(defn- key-by-id [coll]
  (into {} (map (juxt :id identity)) coll))

(defn- transact! [ks f]
  (swap! app-state update-in ks f))

; login state

(defn set-login-state!
  [state]
  (transact! [:login-state] (constantly state)))

; window visibility & notifications

(defn set-window-visibility!
  [visible?]
  (when visible?
    (transact! [:notifications :unread-count] (constantly 0))
    (set! (.-title js/document) "Chat"))
  (transact! [:notifications :window-visible?] (constantly visible?)))

; error

(defn display-error! [err-key msg]
  (transact! [:errors] #(conj % [err-key msg])))

(defn clear-error! [err-key]
  (transact! [:errors] #(into [] (remove (fn [[k _]] (= k err-key))) %)))

; page

(defn set-group-and-page! [group-id page-id]
  (swap! app-state (fn [s] (assoc s
                             :open-group-id group-id
                             :page page-id
                             ))))
; session

(defn set-session! [session]
  (transact! [:session] (constantly session)))

(defn clear-session! []
  (transact! [:session] (constantly nil)))

(defn set-nickname! [nick]
  (transact! [:session :nickname] (constantly nick)))

; users

(defn add-users! [users]
  (transact! [:users] #(merge % (key-by-id users))))

(defn add-user! [user]
  (transact! [:users] #(assoc % (:id user) user)))

(defn update-user-nick! [user-id nick]
  (transact! [:users user-id :nickname] (constantly nick)))

(defn update-user-status! [user-id status]
  (transact! [:users user-id :status] (constantly status)))

(defn all-users []
  (vals (get-in @app-state [:users])))

(defn users-in-group [group-id]
  (filter (fn [u] (contains? (set (u :group-ids)) group-id)) (vals (@app-state :users))))

(defn users-in-open-group []
  (users-in-group (@app-state :open-group-id)))

(defn user-in-open-group? [user-id]
  (contains? (set (get-in @app-state [:users user-id :group-ids])) (@app-state :open-group-id)))

(defn nickname->user [nickname]
  (->> (get-in @app-state [:users])
       vals
       (filter (fn [u] (= nickname (u :nickname))))
       ; nicknames are unique, so take the first
       first))

(defn valid-user-id? [user-id]
  (some? (get-in @app-state [:users user-id])))

; threads and messages

(defn set-new-thread!
  "Hacky way of tracking if a thread has just been created, so we can focus the reply text box"
  [thread-id]
  (transact! [:new-thread-id] (constantly thread-id)))

(defn get-new-thread []
  (@app-state :new-thread-id))

(defn clear-new-thread! []
  (transact! [:new-thread-id] (fn [_] (uuid/make-random-squuid))))

(defn update-thread-last-open-at [thread-id]
  (transact! [:threads thread-id :last-open-at] (constantly (js/Date.))))

(defn set-open-threads! [threads]
  (transact! [:threads] (constantly (key-by-id threads)))
  (transact! [:user :open-thread-ids] (constantly (set (map :id threads)))))

(defn- maybe-create-thread! [thread-id]
  (when-not (get-in @app-state [:threads thread-id])
    (transact! [:threads thread-id] (constantly {:id thread-id
                                                 :messages []
                                                 :tag-ids #{}
                                                 :mentioned-ids #{}} )))
  (transact! [:user :open-thread-ids] #(conj % thread-id)))

(defn add-message! [message]
  (maybe-create-thread! (message :thread-id))
  (transact! [:threads (message :thread-id) :messages] #(conj % message))
  (update-thread-last-open-at (message :thread-id))
  (transact! [:threads (message :thread-id) :tag-ids] #(apply conj (set %) (message :mentioned-tag-ids)))
  (transact! [:threads (message :thread-id) :mentioned-ids] #(apply conj (set %) (message :mentioned-user-ids))))

(defn set-message-failed! [message]
  (transact! [:threads (message :thread-id) :messages]
    (partial map (fn [msg] (if (= (message :id) (msg :id))
                             (assoc msg :failed? true)
                             msg)))))

(defn clear-message-failed! [message]
  (transact! [:threads (message :thread-id) :messages]
    (partial map (fn [msg] (if (= (message :id) (msg :id))
                             (dissoc msg :failed?)
                             msg)))))

(defn add-threads! [threads]
  (transact! [:threads] #(merge % (key-by-id threads))))

(defn add-open-thread! [thread]
  ; TODO move notifications logic out of here
  (when-not (get-in @app-state [:notifications :window-visible?])
    (transact! [:notifications :unread-count] inc)
    (set! (.-title js/document)
          (str "Chat (" (get-in @app-state [:notifications :unread-count]) ")")))

  (transact! [:threads (thread :id)] (constantly thread))
  (transact! [:user :open-thread-ids] #(conj % (thread :id))))

(defn hide-thread! [thread-id]
  (transact! [:user :open-thread-ids] #(disj % thread-id)))

(defn id->thread [thread-id]
  (get-in @app-state [:threads thread-id]))

(defn open-thread? [thread-id]
  (contains? (set (get-in @app-state [:user :open-thread-ids])) thread-id))

(defn set-pagination-remaining! [threads-count]
  (transact! [:pagination-remaining] (constantly threads-count)))

(defn pagination-remaining []
  (get @app-state :pagination-remaining 0))

; channels page

(defn set-channel-results! [threads]
  (transact! [:threads] #(merge % (key-by-id threads)))
  (transact! [:page :thread-ids] (constantly (map :id threads))))

(defn add-channel-results! [threads]
  (transact! [:threads] #(merge % (key-by-id threads)))
  (transact! [:page :thread-ids] #(concat % (map :id threads))))

; search threads

(defn set-search-results! [{:keys [threads thread-ids]}]
  (transact! [:threads] #(merge % (key-by-id threads)))
  (transact! [:page :thread-ids] (constantly thread-ids)))

(defn set-search-query! [query]
  (transact! [:page :search-query] (constantly query)))

; tags

(defn add-tags! [tags]
  (transact! [:tags] #(merge % (key-by-id tags))))

(defn add-tag! [tag]
  (transact! [:tags (tag :id)] (constantly tag)))

(defn remove-tag! [tag-id]
  (transact! [:tags] #(dissoc % tag-id)))

(defn all-tags []
  (vals (get-in @app-state [:tags])))

(defn tags-in-group [group-id]
  (filter #(= group-id (% :group-id)) (vals (@app-state :tags))))

(defn tags-in-open-group []
  (tags-in-group (@app-state :open-group-id)))

(defn tag-in-open-group? [tag-id]
  (= (get-in @app-state [:tags tag-id :group-id]) (@app-state :open-group-id)))

(defn name->open-tag-id
  "Lookup tag by name in the open group"
  [tag-name]
  (let [open-group (@app-state :open-group-id)]
    (->> (@app-state :tags)
         vals
         (filter (fn [t] (and (= open-group (t :group-id)) (= tag-name (t :name)))))
         first
         :id)))

(defn get-tag [tag-id]
  (get-in @app-state [:tags tag-id]))

(defn get-ambiguous-tags
  "Get a set of all ambiguous tag names"
  []
  (let [tag-names (->> @app-state :tags vals (map :name))]
    (set (for [[tag-name freq] (frequencies tag-names)
               :when (> freq 1)]
           tag-name))))

(defn ambiguous-tag?
  "Returns a set of the groups with a tag of the given name if the tag exists
  in multiple groups, or nil if the tag is only present in one or zero groups"
  [tag-name]
  (when (contains? (get-ambiguous-tags) tag-name)
    (->> @app-state :tags vals
         (filter #(= (% :name) tag-name))
         (map #(select-keys % [:group-id :group-name :id])))))

(defn group-for-tag
  "Get the group id for the tag with the given id"
  [tag-id]
  (get-in @app-state [:tags tag-id :group-id]))

; subscribed tags

(defn set-user-subscribed-tag-ids! [tag-ids]
  (transact! [:user :subscribed-tag-ids] (constantly (set tag-ids))))

(defn unsubscribe-from-tag! [tag-id]
  (transact! [:user :subscribed-tag-ids] #(disj % tag-id)))

(defn subscribe-to-tag! [tag-id]
  (transact! [:user :subscribed-tag-ids] #(conj % tag-id)))

(defn is-subscribed-to-tag? [tag-id]
  (contains? (get-in @app-state [:user :subscribed-tag-ids]) tag-id))

; groups

(defn set-open-group! [group-id]
  (transact! [:open-group-id] (constantly group-id)))

(defn open-group-id []
  (get @app-state :open-group-id))

(defn id->group [group-id]
  (get-in @app-state [:groups group-id]))

(defn set-user-joined-groups! [groups]
  (transact! [:groups] (constantly (key-by-id groups))))

(defn add-group! [group]
  (transact! [:groups] (flip assoc (group :id) group)))

(defn remove-group! [group]
  (transact! [:groups] (flip dissoc (group :id))))

; invitations

(defn set-invitations! [invitations]
  (transact! [:invitations] (constantly invitations)))

(defn add-invite! [invite]
  (transact! [:invitations] #(conj % invite)))

(defn remove-invite! [invite]
  (transact! [:invitations] (partial remove (partial = invite))))

; preferences

(defn add-preferences! [prefs]
  (transact! [:preferences] #(merge % prefs)))

(defn user-preferences []
  (get @app-state :preferences))

; inbox

(defn open-threads [state]
  (let [current-group-id (state :open-group-id)
        open-threads (-> (state :threads)
                        (select-keys (get-in @app-state [:user :open-thread-ids]))
                         vals
                         (->> (filter (fn [thread]
                                (or (empty? (thread :tag-ids))
                                    (contains?
                                      (into #{} (map group-for-tag) (thread :tag-ids))
                                      current-group-id))))))]
      open-threads))


