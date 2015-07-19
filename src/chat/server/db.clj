(ns chat.server.db
  (:require [datomic.api :as d]
            [crypto.password.scrypt :as password]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  "datomic:dev://localhost:4334/chat-dev")

(def ^:dynamic *conn* nil)

(defn init!
  "set up schema"
  []
  (d/create-database *uri*)
  @(d/transact (d/connect *uri*)
     [; partition for our data
      {:db/ident :entities
       :db/id #db/id [:db.part/db]
       :db.install/_partition :db.part/db}

      ; user
      {:db/ident :user/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user/email
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user/password-token
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user/avatar
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      ; user - thread
      {:db/ident :user/open-thread
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user/subscribed-thread
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      ; user - tag
      {:db/ident :user/subscribed-tag
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ; message
      {:db/ident :message/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :message/content
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :message/created-at
       :db/valueType :db.type/instant
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      ; message - user
      {:db/ident :message/user
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      ; message - thread
      {:db/ident :message/thread
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ; thread
      {:db/ident :thread/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      ; thread - tag
      {:db/ident :thread/tag
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ; tag
      {:db/ident :tag/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :tag/name
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ]))


(defn uuid []
  (d/squuid))

(defn- db->user [e]
  {:id (:user/id e)
   :email (:user/email e)
   :avatar (:user/avatar e)})

(defn- db->message [e]
  {:id (:message/id e)
   :content (:message/content e)
   :user-id (:user/id (:message/user e))
   :thread-id (:thread/id (:message/thread e))
   :created-at (:message/created-at e)})

(defmacro with-conn
  "Execute the body with *conn* dynamically bound to a new connection."
  [& body]
  `(binding [*conn* (d/connect *uri*)]
     ~@body))

(defn- create-entity!
  "create entity with attrs, return entity"
  [attrs]
  (let [new-id (d/tempid :entities)
        {:keys [db-after tempids]} @(d/transact *conn*
                                                [(assoc attrs :db/id new-id)])]
    (->> (d/resolve-tempid db-after tempids new-id)
         (d/entity db-after))))

(defn create-message! [attrs]
  (let [subscribed-users (d/q '[:find [?user ...]
                                :in $ ?thread-id
                                :where
                                [?user :user/subscribed-thread ?thread]
                                [?thread :thread/id ?thread-id]]
                              (d/db *conn*)
                              (attrs :thread-id))
        ; show thread for all users subscribed to thread
        add-open-transactions (map (fn [user]
                                     [:db/add user
                                      :user/open-thread [:thread/id (attrs :thread-id)]])
                                   subscribed-users)
        ; upsert thread
        thread-data {:db/id (d/tempid :entities)
                     :thread/id (attrs :thread-id)}
        ; upsert message
        msg-data {:db/id (d/tempid :entities)
                  :message/id (attrs :id)
                  :message/content (attrs :content)
                  :message/user [:user/id (attrs :user-id)]
                  :message/thread (:db/id thread-data)
                  :message/created-at (attrs :created-at)}
        ; user who created message: show thread, subscribe to thread
        subscribe-data {:db/id [:user/id (attrs :user-id)]
                        :user/open-thread (thread-data :db/id)
                        :user/subscribed-thread (thread-data :db/id)}
        {:keys [db-after tempids]} @(d/transact *conn* (concat [thread-data
                                                                msg-data
                                                                subscribe-data]
                                                               add-open-transactions))]
    (->> (d/resolve-tempid db-after tempids (msg-data :db/id))
         (d/pull db-after '[:message/id
                            :message/content
                            {:message/user [:user/id]}
                            {:message/thread [:thread/id]}
                            :message/created-at])
         db->message)))

(defn create-user!
  "creates a user, returns id"
  [attrs]
  (-> {:user/id (attrs :id)
       :user/email (attrs :email)
       :user/avatar (attrs :avatar)
       :user/password-token (password/encrypt (attrs :password))}
      create-entity!
      db->user))

(defn authenticate-user
  "returns user-id if email and password are correct"
  [email password]
  (->> (let [[user-id password-token]
             (d/q '[:find [?id ?password-token]
                    :in $ ?email
                    :where
                    [?e :user/id ?id]
                    [?e :user/email ?email]
                    [?e :user/password-token ?password-token]]
                  (d/db *conn*)
                  email)]
         (when (and user-id (password/check password password-token))
           user-id))))

(defn fetch-users []
  (->> (d/q '[:find (pull ?e [:user/id
                              :user/email
                              :user/avatar])
              :where [?e :user/id]]
            (d/db *conn*))
       (map (comp db->user first))))

(defn fetch-messages []
  (->> (d/q '[:find (pull ?e [:message/id
                              :message/content
                              :message/created-at
                              {:message/user [:user/id]}
                              {:message/thread [:thread/id]}])
              :where [?e :message/id]]
            (d/db *conn*))
       (map (comp db->message first))))

(defn get-open-threads-for-user [user-id]
  (d/q '[:find [?thread-id ...]
                :in $ ?user-id
                :where
                [?e :user/id ?user-id]
                [?e :user/open-thread ?thread]
                [?thread :thread/id ?thread-id]]
       (d/db *conn*)
       user-id))

(defn get-subscribed-threads-for-user [user-id]
  (d/q '[:find [?thread-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-thread ?thread]
         [?thread :thread/id ?thread-id]]
       (d/db *conn*)
       user-id))

(defn user-hide-thread! [user-id thread-id]
  (d/transact
    *conn*
    [[:db/retract [:user/id user-id] :user/open-thread [:thread/id thread-id]]]))

(defn- db->tag [e]
  {:id (:tag/id e)
   :name (:tag/name e)})

(defn create-tag! [attrs]
  (-> {:tag/id (attrs :id)
       :tag/name (attrs :name)}
    create-entity!
    db->tag))

(defn user-subscribe-to-tag! [user-id tag-id]
  (d/transact *conn* [[:db/add [:user/id user-id]
                       :user/subscribed-tag [:tag/id tag-id]]]))

(defn get-user-subscribed-tags [user-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-tag ?tag]
         [?tag :tag/id ?tag-id]]
       (d/db *conn*)
       user-id))

(defn thread-add-tag! [thread-id tag-id]
  (d/transact *conn* [[:db/add [:thread/id thread-id]
                       :thread/tag [:tag/id tag-id]]]))

(defn get-thread-tags [thread-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?thread-id
         :where
         [?thread :thread/id ?thread-id]
         [?thread :thread/tag ?tag]
         [?tag :tag/id ?tag-id]]
       (d/db *conn*)
       thread-id))

(defn fetch-tags []
  (->> (d/q '[:find (pull ?e [:tag/id
                              :tag/name])
              :where [?e :tag/id]]
            (d/db *conn*))
       (map (comp db->tag first))))
