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


      ]))


(defn- db->user [e]
  {:id (:user/id e)
   :email (:user/email e)
   :avatar (:user/avatar e)})

(defn- db->message [resp]
  {:id (resp :message/id)
   :content (resp :message/content)
   :user-id (resp :message/user-id)
   :thread-id (resp :message/thread-id)
   :created-at (resp :message/created-at)})

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

(defn- create-reply-message! [attrs]
  (create-entity! {:message/id (attrs :id)
                   :message/content (attrs :content)
                   :message/user [:user/id (attrs :user-id)]
                   :message/thread [:thread/id (attrs :thread-id)]
                   :message/created-at (attrs :created-at)}))

(defn- create-first-message! [attrs]
  (create-entity! {:message/id (attrs :id)
                   :message/content (attrs :content)
                   :message/user [:user/id (attrs :user-id)]
                   :message/thread (d/tempid :entities)
                   :message/created-at (attrs :created-at)}))

(defn uuid []
  (d/squuid))

(defn create-message! [attrs]
  (if (attrs :thread-id)
    (create-reply-message! attrs)
    (create-first-message! attrs)))

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
  (->> (d/q '[:find (pull ?e [:user/id :user/email :user/avatar])
              :where [?e :user/id]]
            (d/db *conn*))
       (map (comp db->user first))))

(defn fetch-messages []
  )
