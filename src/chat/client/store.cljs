(ns chat.client.store
  (:require [cljs-utils.core :refer [flip]]))

(defonce app-state
  (atom {:threads {}
         :users {}
         :tags {}
         :groups {}
         :page {:type :home}
         :session nil
         :error-msg nil
         :invitations []
         :notifications {:window-visible? true
                         :unread-count 0}
         :user {:open-thread-ids #{}
                :subscribed-tag-ids #{}
                :user-id nil
                :nickname nil}}))

(defn- key-by-id [coll]
  (reduce (fn [memo x]
            (assoc memo (x :id) x)) {} coll))

(defn- transact! [ks f]
  (swap! app-state update-in ks f))

; window visibility & notifications

(defn set-window-visibility!
  [visible?]
  (when visible?
    (transact! [:notifications :unread-count] (constantly 0))
    (set! (.-title js/document) "Chat"))
  (transact! [:notifications :window-visible?] (constantly visible?)))

; error

(defn display-error! [msg]
  (transact! [:error-msg] (constantly msg)))

(defn clear-error! []
  (transact! [:error-msg] (constantly nil)))

; page

(defn set-page! [page]
  (transact! [:page] (constantly page)))

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

(defn nickname->user [nickname]
  (->> (get-in @app-state [:users])
       vals
       (filter (fn [u] (= nickname (u :nickname))))
       first))

; threads and messages

(defn set-open-threads! [threads]
  (transact! [:threads] (constantly (key-by-id threads)))
  (transact! [:user :open-thread-ids] (constantly (set (map :id threads)))))

(defn- maybe-create-thread! [thread-id]
  (when-not (get-in @app-state [:threads thread-id])
    (transact! [:threads thread-id] (constantly {:id thread-id
                                                 :messages []
                                                 :tag-ids #{}})))
  (transact! [:user :open-thread-ids] #(conj % thread-id)))

(defn add-message! [message]
  (maybe-create-thread! (message :thread-id))
  (transact! [:threads (message :thread-id) :messages] #(conj % message))

  (transact! [:threads (message :thread-id) :tags] #(conj % (message :mentioned-tag-ids)))
  (transact! [:threads (message :thread-id) :mentioned-ids] #(conj % (message :mentioned-user-ids))))

(defn add-open-thread! [thread]
  ; TODO move notifications logic out of here
  (when-not (get-in @app-state [:notifications :window-visible?])
    (transact! [:notifications :unread-count] inc)
    (set! (.-title js/document)
          (str "Chat (" (get-in @app-state [:notifications :unread-count]) ")")))

  (transact! [:threads (thread :id)] (constantly thread))
  (transact! [:user :open-thread-ids] #(conj % (thread :id))))

(defn hide-thread! [thread-id]
  (transact! [:threads] #(dissoc % thread-id))
  (transact! [:user :open-thread-ids] #(disj % thread-id)))

(defn id->thread [thread-id]
  (get-in @app-state [:threads thread-id]))

; search threads

(defn set-search-results! [threads]
  (transact! [:threads] #(merge % (key-by-id threads)))
  (transact! [:page :search-result-ids] (constantly (map :id threads))))

(defn set-search-searching! [bool]
  (transact! [:page :search-searching] (constantly bool)))

(defn set-search-query! [query]
  (transact! [:page :search-query] (constantly query)))

; tags

(defn add-tags! [tags]
  (transact! [:tags] #(merge % (key-by-id tags))))

(defn add-tag! [tag]
  (transact! [:tags (tag :id)] (constantly tag)))

(defn all-tags []
  (vals (get-in @app-state [:tags])))

(defn tags-in-group [group-id]
  (filter #(= group-id (% :group-id)) (vals (@app-state :tags))))

(defn name->tag [tag-name]
  (->> (@app-state :tags)
      vals
      (filter (fn [t] (= tag-name (t :name))))
      first))

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
