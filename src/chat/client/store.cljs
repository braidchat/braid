(ns chat.client.store
  (:require [cljs-utils.core :refer [flip]]))

(def app-state (atom {:threads {}
                      :users {}
                      :tags {}
                      :groups {}
                      :session nil
                      :error-msg nil
                      :invitations []
                      :user {:open-thread-ids #{}
                             :subscribed-tag-ids #{}
                             :user-id nil}
                      :open-thread-ids #{}}))

(defn- key-by-id [coll]
  (reduce (fn [memo x]
            (assoc memo (x :id) x)) {} coll))

(defn- transact! [ks f]
  (swap! app-state update-in ks f))

; error

(defn display-error! [msg]
  (transact! [:error-msg] (constantly msg)))

(defn clear-error! []
  (transact! [:error-msg] (constantly nil)))

; session

(defn set-session! [session]
  (transact! [:session] (constantly session)))

(defn clear-session! []
  (transact! [:session] (constantly nil)))

; users


(defn add-users! [users]
  (transact! [:users] #(merge % (key-by-id users))))

; threads and messages

(defn set-threads! [threads]
  (transact! [:threads] (constantly (key-by-id threads))))

(defn- maybe-create-thread! [thread-id]
  (when-not (get-in @app-state [:threads thread-id])
    (transact! [:threads thread-id] (constantly {:id thread-id
                                                 :messages []
                                                 :tag-ids #{}}))))

(defn add-message! [message]
  (maybe-create-thread! (message :thread-id))
  (transact! [:threads (message :thread-id) :messages] #(conj % message)))

(defn add-thread! [thread]
  (transact! [:threads (thread :id)] (constantly thread)))

(defn hide-thread! [thread-id]
  (transact! [:threads] #(dissoc % thread-id)))

(defn id->thread [thread-id]
  (get-in @app-state [:threads thread-id]))

; tags

(defn add-tags! [tags]
  (transact! [:tags] #(merge % (key-by-id tags))))

(defn add-tag! [tag]
  (transact! [:tags (tag :id)] (constantly tag)))

(defn add-tag-to-thread! [tag-id thread-id]
  (maybe-create-thread! thread-id)
  (transact! [:threads thread-id :tag-ids] #(set (conj % tag-id))))

(defn all-tags []
  (vals (get-in @app-state [:tags])))

(defn tag-id-for-name
  "returns id for tag with name tag-name if exists, otherwise nil"
  [tag-name]
  (-> (@app-state :tags)
      vals
      (->> (filter (fn [t] (= tag-name (t :name)))))
      first
      (get :id)))

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
