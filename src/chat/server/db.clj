(ns chat.server.db
  (:require [datomic.api :as d]
            [environ.core :refer [env]]
            [clojure.string :as string]
            [crypto.password.scrypt :as password]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  (get env :db-url "datomic:dev://localhost:4334/chat-dev"))

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
       :db/unique :db.unique/value
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user/nickname
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/value
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
       :db/fulltext true
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
      ; thread - mentions
      {:db/ident :thread/mentioned
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
      {:db/ident :tag/group
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ; groups
      {:db/ident :group/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :group/name
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :group/user
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ; invitations
      {:db/ident :invite/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :invite/group
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :invite/from
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :invite/to
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :invite/created-at
       :db/valueType :db.type/instant
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      ]))


(defn uuid
  []
  (d/squuid))

(defn- db->user
  [e]
  {:id (:user/id e)
   :nickname (:user/nickname e)
   :email (:user/email e)
   :avatar (:user/avatar e)})

(defn- db->message
  [e]
  {:id (:message/id e)
   :content (:message/content e)
   :user-id (:user/id (:message/user e))
   :thread-id (:thread/id (:message/thread e))
   :created-at (:message/created-at e)})

(defn- db->group [e]
  {:id (:group/id e)
   :name (:group/name e)
   :users (:group/user e)})

(defn- db->invitation [e]
  {:id (:invite/id e)
   :inviter-id (get-in e [:invite/from :user/id])
   :inviter-email (get-in e [:invite/from :user/email])
   :inviter-nickname (get-in e [:invite/from :user/nickname])
   :invitee-email (:invite/to e)
   :group-id (get-in e [:invite/group :group/id])
   :group-name (get-in e [:invite/group :group/name])})

(defn- db->tag
  [e]
  {:id (:tag/id e)
   :name (:tag/name e)
   :group-id (get-in e [:tag/group :group/id])
   :group-name (get-in e [:tag/group :group/name])})

(defn- db->thread
  [thread]
  {:id (thread :thread/id)
   :messages (map (fn [msg]
                    {:id (msg :message/id)
                     :content (msg :message/content)
                     :user-id (get-in msg [:message/user :user/id])
                     :created-at (msg :message/created-at)})
                  (thread :message/_thread))
   :tag-ids (map :tag/id (thread :thread/tag))
   :mentioned-ids (map :user/id (thread :thread/mentioned))})

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

(defn get-users-subscribed-to-thread
  [thread-id]
  (d/q '[:find [?user-id ...]
         :in $ ?thread-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-thread ?thread]
         [?thread :thread/id ?thread-id]]
       (d/db *conn*)
       thread-id))

(defn create-message!
  [{:keys [thread-id id content user-id created-at]}]
  (let [; show thread for all users subscribed to thread
        add-open-transactions (map (fn [user-id]
                                     [:db/add [:user/id user-id]
                                      :user/open-thread [:thread/id thread-id]])
                                   (get-users-subscribed-to-thread thread-id))
        ; upsert thread
        thread-data {:db/id (d/tempid :entities)
                     :thread/id thread-id}
        ; upsert message
        msg-data {:db/id (d/tempid :entities)
                  :message/id id
                  :message/content content
                  :message/user [:user/id user-id]
                  :message/thread (:db/id thread-data)
                  :message/created-at created-at}
        ; user who created message: show thread, subscribe to thread
        subscribe-data {:db/id [:user/id user-id]
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

(defn group-exists?
  [group-name]
  (some? (d/pull (d/db *conn*) '[:group/id] [:group/name group-name])))

(defn create-group!
  [{:keys [name id]}]
  (-> {:group/id id
       :group/name name}
      create-entity!
      db->group))

(defn email-taken?
  [email]
  (-> (d/q '[:find ?e :in $ ?email :where [?e :user/email ?email]] (d/db *conn*) email)
      first
      some?))

(defn create-user!
  "creates a user, returns id"
  [{:keys [id email avatar nickname password]}]
  (-> {:user/id id
       :user/email email
       :user/avatar avatar
       :user/nickname (or nickname (-> email (string/split #"@") first))
       :user/password-token (password/encrypt password)}
      create-entity!
      db->user))

(defn nickname-taken?
  [nickname]
  (some? (d/entity (d/db *conn*) [:user/nickname nickname])))

(defn set-nickname!
  "Set the user's nickname"
  [user-id nickname]
  (d/transact *conn* [[:db/add [:user/id user-id] :user/nickname nickname]]))

(defn get-nickname
  [user-id]
  (:user/nickname (d/pull (d/db *conn*) '[:user/nickname] [:user/id user-id])))

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

(defn user-by-id
  [id]
  (some-> (d/pull (d/db *conn*) '[:user/id :user/avatar :user/nickname] [:user/id id])
          db->user))

(defn user-with-email
  "get the user with the given email address or nil if no such user registered"
  [email]
  (some-> (d/pull (d/db *conn*) '[:user/id :user/email :user/avatar :user/nickname] [:user/email email])
          db->user))

(defn create-invitation!
  [{:keys [id inviter-id invitee-email group-id]}]
  (-> {:invite/id id
       :invite/group [:group/id group-id]
       :invite/from [:user/id inviter-id]
       :invite/to invitee-email
       :invite/created-at (java.util.Date.)}
      create-entity!
      db->invitation))

(defn get-invite
  [invite-id]
  (some-> (d/pull (d/db *conn*)
              [:invite/id
               {:invite/from [:user/id :user/email :user/nickname]}
               :invite/to
               {:invite/group [:group/id :group/name]}]
              [:invite/id invite-id])
      db->invitation))

(defn retract-invitation!
  [invite-id]
  (d/transact *conn* [[:db.fn/retractEntity [:invite/id invite-id]]]))

(defn fetch-users-for-user
  "Get all users visible to given user"
  [user-id]
  (->> (d/q '[:find (pull ?e [:user/id
                              :user/email
                              :user/nickname
                              :user/avatar])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?g :group/user ?e]]
            (d/db *conn*) user-id)
       (map (comp db->user first))
       set))

(defn user-visible-to-user?
  "Are the two user ids users that can see each other? i.e. do they have at least one group in common"
  [user1-id user2-id]
  (-> (d/q '[:find ?g
         :in $ ?u1-id ?u2-id
         :where
         [?u1 :user/id ?u1-id]
         [?u2 :user/id ?u2-id]
         [?g :group/user ?u1]
         [?g :group/user ?u2]]
       (d/db *conn*) user1-id user2-id)
      seq boolean))

(defn fetch-invitations-for-user
  [user-id]
  (->> (d/q '[:find (pull ?i [{:invite/group [:group/id :group/name]}
                              {:invite/from [:user/id :user/email :user/nickname]}
                              :invite/id])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?u :user/email ?email]
              [?i :invite/to ?email]]
            (d/db *conn*) user-id)
       (map (comp db->invitation first))))

(defn get-open-thread-ids-for-user
  [user-id]
  (d/q '[:find [?thread-id ...]
                :in $ ?user-id
                :where
                [?e :user/id ?user-id]
                [?e :user/open-thread ?thread]
                [?thread :thread/id ?thread-id]]
       (d/db *conn*)
       user-id))

(defn get-group
  [group-id]
  (-> (d/pull (d/db *conn*) [:group/id :group/name] [:group/id group-id])
      db->group))

(defn get-groups-for-user [user-id]
  (->> (d/q '[:find (pull ?g [:group/id :group/name])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]]
            (d/db *conn*)
            user-id)
       (map (comp #(dissoc % :users) db->group first))
       set))

(defn get-users-in-group [group-id]
  (->> (d/q '[:find (pull ?u [:user/id :user/email :user/nickname :user/avatar])
              :in $ ?group-id
              :where
              [?g :group/id ?group-id]
              [?g :group/user ?u]]
            (d/db *conn*)
            group-id)
       (map (comp db->user first))
       set))

(defn get-user-visible-tag-ids
  [user-id]
  (->> (d/q '[:find ?tag-id
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?t :tag/group ?g]
              [?t :tag/id ?tag-id]]
            (d/db *conn*) user-id)
       (map first)
       set))

(defn get-open-threads-for-user
  [user-id]
  (let [visible-tags (get-user-visible-tag-ids user-id)]
    (->> (d/q '[:find (pull ?thread [:thread/id
                                     {:thread/tag [:tag/id]}
                                     {:thread/mentioned [:user/id]}
                                     {:message/_thread [:message/id
                                                        :message/content
                                                        {:message/user [:user/id]}
                                                        :message/created-at]}])
                :in $ ?user-id
                :where
                [?e :user/id ?user-id]
                [?e :user/open-thread ?thread]]
              (d/db *conn*) user-id)
         (into ()
               (map (comp (fn [t]
                            (update-in t [:tag-ids] (partial filter visible-tags)))
                          db->thread
                          first))))))

(defn get-thread
  [thread-id]
  (-> (d/q '[:find (pull ?thread [:thread/id
                                  {:thread/tag [:tag/id]}
                                  {:thread/mentioned [:user/id]}
                                  {:message/_thread [:message/id
                                                     :message/content
                                                     {:message/user [:user/id]}
                                                     :message/created-at]}])
             :in $ ?thread-id
             :where
             [?thread :thread/id ?thread-id]]
           (d/db *conn*)
           thread-id)
      first
      first
      db->thread))

(defn get-subscribed-thread-ids-for-user
  [user-id]
  (d/q '[:find [?thread-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-thread ?thread]
         [?thread :thread/id ?thread-id]]
       (d/db *conn*)
       user-id))

(defn user-hide-thread!
  [user-id thread-id]
  (d/transact
    *conn*
    [[:db/retract [:user/id user-id] :user/open-thread [:thread/id thread-id]]]))

(defn create-tag! [attrs]
  (-> {:tag/id (attrs :id)
       :tag/name (attrs :name)
       :tag/group [:group/id (attrs :group-id)]}
      create-entity!
      db->tag))

(defn user-in-group?
  [user-id group-id]
  (seq (d/q '[:find ?g
              :in $ ?user-id ?group-id
              :where
              [?u :user/id ?user-id]
              [?g :group/id ?group-id]
              [?g :group/user ?u]]
            (d/db *conn*)
            user-id group-id)))

(defn user-in-tag-group? [user-id tag-id]
  (seq (d/q '[:find ?g
              :in $ ?user-id ?tag-id
              :where
              [?u :user/id ?user-id]
              [?t :tag/id ?tag-id]
              [?t :tag/group ?g]
              [?g :group/user ?u]]
            (d/db *conn*)
            user-id tag-id)))

(defn user-can-see-thread?
  [user-id thread-id]
  (or
    ;user can see the thread if it's a new (i.e. not yet in the database) thread...
    (empty? (d/q '[:find ?t :in $ ?thread-id
                   :where [?t :thread/id ?thread-id]]
                 (d/db *conn*) thread-id))
    ; ...or they're already subscribed to the thread...
    (contains? (set (get-users-subscribed-to-thread thread-id)) user-id)
    ; ...or they're mentioned in the thread
    ; TODO: is it possible for them to be mentioned but not subscribed?
    (contains? (-> (d/pull (d/db *conn*) [:thread/mentioned] [:thread/id thread-id])
                   :thread/mentioned set)
               user-id)
    ; ...or they are in the group of any tags on the thread
    (seq (d/q '[:find (pull ?group [:group/id])
                :in $ ?thread-id ?user-id
                :where
                [?thread :thread/id ?thread-id]
                [?thread :thread/tag ?tag]
                [?tag :tag/group ?group]
                [?group :group/user ?user]
                [?user :user/id ?user-id]]
              (d/db *conn*) thread-id user-id))))

(defn user-subscribe-to-tag! [user-id tag-id]
  ; TODO: throw an exception/some sort of error condition if user tried to
  ; subscribe to a tag they can't?
  (when (user-in-tag-group? user-id tag-id)
    (d/transact *conn* [[:db/add [:user/id user-id]
                         :user/subscribed-tag [:tag/id tag-id]]])))

(defn user-unsubscribe-from-tag!
  [user-id tag-id]
  (d/transact *conn* [[:db/retract [:user/id user-id]
                       :user/subscribed-tag [:tag/id tag-id]]]))

(defn user-add-to-group! [user-id group-id]
  (d/transact *conn* [[:db/add [:group/id group-id]
                       :group/user [:user/id user-id]]]))

(defn user-subscribe-to-group-tags!
  "Subscribe the user to all current tags in the group"
  [user-id group-id]
  (->> (d/q '[:find ?tag
              :in $ ?group-id
              :where
              [?tag :tag/group ?g]
              [?g :group/id ?group-id]]
            (d/db *conn*) group-id)
       (map (fn [[tag]]
              [:db/add [:user/id user-id]
               :user/subscribed-tag tag]))
       (d/transact *conn*)))

(defn get-user-subscribed-tag-ids
  [user-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-tag ?tag]
         [?tag :tag/id ?tag-id]]
       (d/db *conn*)
       user-id))

(defn- get-users-subscribed-to-tag
  [tag-id]
  (d/q '[:find [?user-id ...]
         :in $ ?tag-id
         :where
         [?tag :tag/id ?tag-id]
         [?user :user/subscribed-tag ?tag]
         [?user :user/id ?user-id]]
       (d/db *conn*)
       tag-id))

(defn thread-add-tag!
  [thread-id tag-id]
  (let [subscriber-transactions
        (mapcat (fn [user-id]
                  [[:db/add [:user/id user-id]
                    :user/subscribed-thread [:thread/id thread-id]]
                   [:db/add [:user/id user-id]
                    :user/open-thread [:thread/id thread-id]]])
                (get-users-subscribed-to-tag tag-id))]
    ; upsert thread (should be a way to do in same transaction?)
    (d/transact *conn* (concat [{:db/id (d/tempid :entities)
                                 :thread/id thread-id}]))
    ; add tag, subscribers
    (d/transact *conn* (concat [[:db/add [:thread/id thread-id]
                                 :thread/tag [:tag/id tag-id]]]
                               subscriber-transactions))))

(defn thread-add-mentioned!
  [thread-id user-id]
  ; TODO: upsert thread if needed
  @(d/transact *conn*
    [[:db/add [:thread/id thread-id] :thread/mentioned [:user/id user-id]]
     [:db/add [:user/id user-id] :user/subscribed-thread [:thread/id thread-id]]
     [:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]]))

(defn get-thread-tags
  "Only used for testing"
  [thread-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?thread-id
         :where
         [?thread :thread/id ?thread-id]
         [?thread :thread/tag ?tag]
         [?tag :tag/id ?tag-id]]
       (d/db *conn*)
       thread-id))

(defn get-group-tags
  [group-id]
  (->> (d/q '[:find (pull ?t [:tag/id
                              :tag/name
                              {:tag/group [:group/id :group/name]}])
              :in $ ?group-id
              :where
              [?g :group/id ?group-id]
              [?t :tag/group ?g]]
            (d/db *conn*) group-id)
       (map (comp db->tag first))))

(defn fetch-tags-for-user
  "Get all tags visible to the given user"
  [user-id]
  (->> (d/q '[:find (pull ?e [:tag/id
                              :tag/name
                              {:tag/group [:group/id :group/name]}])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?e :tag/group ?g]]
            (d/db *conn*) user-id)
       (map (comp db->tag first))
       set))
