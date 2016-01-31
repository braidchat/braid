(ns chat.server.schema)

(def schema
 [ ; user
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
])
