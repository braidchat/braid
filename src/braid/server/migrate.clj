(ns braid.server.migrate
  (:require [braid.server.db :as db]
            [datomic.api :as d]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.edn :as edn]))

(defn migrate-2016-08-18
  "schema change for quests"
  []
  @(d/transact db/conn
     [
      ; quest-records
      {:db/ident :quest-record/id
       :db/valueType :db.type/uuid
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/identity
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :quest-record/quest-id
       :db/valueType :db.type/keyword
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :quest-record/user
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :quest-record/state
       :db/valueType :db.type/keyword
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :quest-record/progress
       :db/valueType :db.type/long
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}])

  (let [user-ids (->> (d/q '[:find ?user-id
                             :in $
                             :where
                             [?u :user/id ?user-id]]
                           (d/db db/conn))
                      (map first))]
    (doseq [user-id user-ids]
      (db/activate-first-quests! user-id))))

(defn migrate-2016-07-29
  "Add watched threads for bots"
  []
  (d/transact db/conn
    [{:db/ident :bot/watched
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/many
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}]))

(defn migrate-2016-06-27
  "Add uploads schema"
  []
  (d/transact db/conn
    [{:db/ident :upload/id
      :db/valueType :db.type/uuid
      :db/cardinality :db.cardinality/one
      :db/unique :db.unique/identity
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :upload/thread
      :db/doc "The thread this upload is associated with"
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :upload/url
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :upload/uploaded-at
      :db/valueType :db.type/instant
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :upload/uploaded-by
      :db/doc "User that uploaded this file"
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}]))

(defn migrate-2016-06-08
  "Add bot schema"
  []
  (d/transact db/conn
    [{:db/ident :user/is-bot?
      :db/valueType :db.type/boolean
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}

     {:db/ident :bot/id
      :db/valueType :db.type/uuid
      :db/cardinality :db.cardinality/one
      :db/unique :db.unique/identity
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/token
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/name
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/avatar
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/webhook-url
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/group
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     {:db/ident :bot/user
      :db/doc "Fake user bot posts under"
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/id #db/id [:db.part/db]
      :db.install/_attribute :db.part/db}
     ]))

(defn migrate-2016-06-04
  "Remove old-style extensions"
  []
  (->> (d/q '[:find [?e ...]
              :where
              [?e :extension/id]]
            (d/db db/conn))
       (mapv (fn [e] [:db.fn/retractEntity e]))
       (d/transact db/conn)
       deref))

(defn migrate-2016-05-13
  "Threads have associated groups"
  []
  @(d/transact db/conn
     [{:db/ident :thread/group
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}])
  (let [threads (->> (d/q '[:find [?t ...]
                            :where
                            [?t :thread/id]]
                          (d/db db/conn))
                     (d/pull-many
                       (d/db db/conn)
                       [:thread/id
                        {:thread/mentioned [:user/id]}
                        {:thread/tag [:tag/group]}
                        {:message/_thread [:message/created-at
                                           {:message/user [:user/id]}]}]))
        tx
        (vec
          (for [th threads]
            (let [author (->> (th :message/_thread)
                              (sort-by :message/created-at)
                              first
                              :message/user)
                  author-grp (some-> author :user/id
                                     db/user-groups
                                     first :id)
                  fallback-group (:group/id (d/pull (d/db db/conn) [:group/id] [:group/name "Braid"]))]
              (cond
                (seq (th :thread/tag))
                (let [grp (get-in th [:thread/tag 0 :tag/group :db/id])]
                  (when (nil? grp)
                    (println "Nil by tag " (th :thread/id)))
                  [:db/add [:thread/id (th :thread/id)]
                   :thread/group grp])

                (seq (th :thread/mentioned))
                (let [grps (apply
                             set/intersection
                             (map (comp :id
                                        db/user-groups
                                        :user/id)
                                  (cons author (th :thread/mentioned))))
                      grp (or (first grps) author-grp fallback-group)]
                  (when (nil? grp)
                    (println "Nil by mentions " (th :thread/id)))
                  [:db/add [:thread/id (th :thread/id)]
                   :thread/group [:group/id grp]])

                :else
                (let [grp (or author-grp fallback-group)]
                  (when (nil? grp)
                    (println "nil by author" (th :thread/id)))
                  [:db/add [:thread/id (th :thread/id)]
                   :thread/group [:group/id grp]])))))]
    @(d/transact db/conn tx)))

(defn migrate-2016-05-07
  "Change how user preferences are stored"
  []
  ; rename old prefs
  @(d/transact db/conn [{:db/id :user/preferences
                         :db/ident :user/preferences-old}])
  ; create new entity type
  @(d/transact db/conn
     [{:db/ident :user/preferences
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}

      {:db/ident :user.preference/key
       :db/valueType :db.type/keyword
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}
      {:db/ident :user.preference/value
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}])

  ; migrate to new style
  (let [prefs (d/q '[:find (pull ?u [:user/id :user/preferences-old])
                     :where [?u :user/id]]
                   (d/db db/conn))]
    (doseq [[p] prefs]
      (let [u-id (:user/id p)
            u-prefs (edn/read-string (:user/preferences-old p))]
        (doseq [[k v] u-prefs]
          (when k (db/user-set-preference! u-id k v)))))))

(defn migrate-2016-05-03
  "Add tag descriptions"
  []
  @(d/transact db/conn
     [{:db/ident :tag/description
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-04-29
  "Add group admins"
  []
  @(d/transact db/conn
     [{:db/ident :group/admins
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-03-28
  "Add group settings"
  []
  @(d/transact db/conn
     [{:db/ident :group/settings
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-03-21
  "Add user preferences"
  []
  @(d/transact db/conn
     [{:db/ident :user/preferences
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-03-04
  "Add extension type as attribute"
  []
  @(d/transact db/conn
     [{:db/ident :extension/type
       :db/valueType :db.type/keyword
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-03-02
  "Add extension user"
  []
  @(d/transact db/conn
     [{:db/ident :extension/user
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2016-02-26
  "Add extension schema"
  []
  @(d/transact db/conn
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
      ]))

(defn migrate-2016-01-14
  "All users must have a nickname"
  []
  (let [give-nicks (->> (d/q '[:find (pull ?u [:user/id :user/email :user/nickname])
                               :where
                               [?u :user/id]]
                             (d/db db/conn))
                        (map first)
                        (filter (comp nil? :user/nickname))
                        (mapv (fn [u] [:db/add [:user/id (:user/id u)]
                                       :user/nickname (-> (:user/email u) (string/split #"@") first)])))]
    @(d/transact db/conn give-nicks)))

(defn migrate-2016-01-01
  "Change email uniqueness to /value, add thread mentions"
  []
  @(d/transact db/conn
     [{:db/id :user/email
       :db/unique :db.unique/value
       :db.alter/_attribute :db.part/db}
      {:db/ident :thread/mentioned
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/many
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2015-12-19
  "Add user nicknames"
  []
  @(d/transact db/conn
     [{:db/ident :user/nickname
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/unique :db.unique/value
       :db/id #db/id [:db.part/db]
       :db.install/_attribute :db.part/db}]))

(defn migrate-2015-12-12
  "Make content fulltext"
  []
  ; rename content
  @(d/transact db/conn [{:db/id :message/content :db/ident :message/content-old}])
  @(d/transact db/conn [{:db/ident :message/content
                         :db/valueType :db.type/string
                         :db/fulltext true
                         :db/cardinality :db.cardinality/one
                         :db/id #db/id [:db.part/db]
                         :db.install/_attribute :db.part/db}])
  (let [messages (->> (d/q '[:find (pull ?e [:message/id
                                             :message/content-old
                                             :message/created-at
                                             {:message/user [:user/id]}
                                             {:message/thread [:thread/id]}])
                             :where [?e :message/id]]
                           (d/db db/conn))
                      (map first))]
    (let [msg-tx (->> messages
                      (map (fn [msg]
                             [:db/add [:message/id (msg :message/id)]
                              :message/content (msg :message/content-old)])))]
      @(d/transact db/conn (doall msg-tx)))))

(defn migrate-2015-07-29
  "Schema changes for groups"
  []
  @(d/transact db/conn
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
       :db.install/_attribute :db.part/db}])
  (println "You'll now need to create a group and add existing users & tags to that group"))

(defn create-group-for-users-and-tags
  "Helper function for migrate-2015-07-29 - give a group name to create that
  group and add all existing users and tags to that group"
  [group-name]
  (let [group (db/create-group! {:id (db/uuid) :name group-name})
        all-users (->> (d/q '[:find ?u :where [?u :user/id]] (d/db db/conn)) (map first))
        all-tags (->> (d/q '[:find ?t :where [?t :tag/id]] (d/db db/conn)) (map first))]
    @(d/transact db/conn (mapv (fn [u] [:db/add [:group/id (group :id)] :group/user u]) all-users))
    @(d/transact db/conn (mapv (fn [t] [:db/add t :tag/group [:group/id (group :id)]]) all-tags))))

(defn migrate-2015-08-26
  "schema change for invites"
  []
  @(d/transact db/conn
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
      ]))

