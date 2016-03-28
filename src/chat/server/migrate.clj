(ns chat.server.migrate
  (:require [chat.server.db :as db]
            [datomic.api :as d]
            [clojure.string :as string]))

(defn migrate-2016-03-21
  "Add user preferences"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :user/preferences
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-04
  "Add extension type as attribute"
  []
  (db/with-conn
    (d/transact db/*conn*
                [{:db/ident :extension/type
                 :db/valueType :db.type/keyword
                 :db/cardinality :db.cardinality/one
                 :db/id #db/id [:db.part/db]
                 :db.install/_attribute :db.part/db}])))

(defn migrate-2016-03-02
  "Add extension user"
  []
  (db/with-conn
    (d/transact db/*conn*
                [{:db/ident :extension/user
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one
                 :db/id #db/id [:db.part/db]
                 :db.install/_attribute :db.part/db}])))

(defn migrate-2016-02-26
  "Add extension schema"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :extension/id
        :db/valueType :db.type/uuid
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/identity
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/group
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/token
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/refresh-token
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/config
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       {:db/ident :extension/watched-threads
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}
       ])))

(defn migrate-2016-01-14
  "All users must have a nickname"
  []
  (db/with-conn
    (let [give-nicks (->> (d/q '[:find (pull ?u [:user/id :user/email :user/nickname])
                                 :where
                                 [?u :user/id]]
                               (d/db db/*conn*))
                          (map first)
                          (filter (comp nil? :user/nickname))
                          (mapv (fn [u] [:db/add [:user/id (:user/id u)]
                                         :user/nickname (-> (:user/email u) (string/split #"@") first)])))]
      (d/transact db/*conn* give-nicks))))

(defn migrate-2016-01-01
  "Change email uniqueness to /value, add thread mentions"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/id :user/email
        :db/unique :db.unique/value
        :db.alter/_attribute :db.part/db}
       {:db/ident :thread/mentioned
        :db/valueType :db.type/ref
        :db/cardinality :db.cardinality/many
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2015-12-19
  "Add user nicknames"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :user/nickname
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/unique :db.unique/value
        :db/id #db/id [:db.part/db]
        :db.install/_attribute :db.part/db}])))

(defn migrate-2015-12-12
  "Make content fulltext"
  []
  ; rename content
  (db/with-conn (d/transact db/*conn* [{:db/id :message/content :db/ident :message/content-old}]))
  (db/with-conn (d/transact db/*conn* [{:db/ident :message/content
                                        :db/valueType :db.type/string
                                        :db/fulltext true
                                        :db/cardinality :db.cardinality/one
                                        :db/id #db/id [:db.part/db]
                                        :db.install/_attribute :db.part/db}]))
  (let [messages (db/with-conn (->> (d/q '[:find (pull ?e [:message/id
                                                           :message/content-old
                                                           :message/created-at
                                                           {:message/user [:user/id]}
                                                           {:message/thread [:thread/id]}])
                                           :where [?e :message/id]]
                                         (d/db db/*conn*))
                                    (map first)))]
    (db/with-conn
      (let [msg-tx (->> messages
                        (map (fn [msg]
                               [:db/add [:message/id (msg :message/id)]
                                :message/content (msg :message/content-old)])))]
        (d/transact db/*conn* (doall msg-tx))))))

(defn migrate-2015-07-29
  "Schema changes for groups"
  []
  (db/with-conn
    (d/transact db/*conn*
      [{:db/ident :tag/group
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
        :db.install/_attribute :db.part/db}]))
  (println "You'll now need to create a group and add existing users & tags to that group"))

(defn create-group-for-users-and-tags
  "Helper function for migrate-2015-07-29 - give a group name to create that
  group and add all existing users and tags to that group"
  [group-name]
  (db/with-conn
    (let [group (db/create-group! {:id (db/uuid) :name group-name})
          all-users (->> (d/q '[:find ?u :where [?u :user/id]] (d/db db/*conn*)) (map first))
          all-tags (->> (d/q '[:find ?t :where [?t :tag/id]] (d/db db/*conn*)) (map first))]
      (d/transact db/*conn* (mapv (fn [u] [:db/add [:group/id (group :id)] :group/user u]) all-users))
      (d/transact db/*conn* (mapv (fn [t] [:db/add t :tag/group [:group/id (group :id)]]) all-tags)))))

(defn migrate-2015-08-26
  "schema change for invites"
  []
  (db/with-conn
    (d/transact db/*conn*
      [
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
       ])))
