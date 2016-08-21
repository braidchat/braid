(ns braid.server.schema)

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
 ; user - preferences
 {:db/ident :user/preferences
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/many
  :db/id #db/id [:db.part/db]
  :db.install/_attribute :db.part/db}
 {:db/ident :user/is-bot?
  :db/valueType :db.type/boolean
  :db/cardinality :db.cardinality/one
  :db/id #db/id [:db.part/db]
  :db.install/_attribute :db.part/db}

 ; user preference - key
 {:db/ident :user.preference/key
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/one
  :db/id #db/id [:db.part/db]
  :db.install/_attribute :db.part/db}
 ; user preference - value
 {:db/ident :user.preference/value
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
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

 ; upload
 {:db/ident :upload/id
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
  ; thread - group
  {:db/ident :thread/group
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
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
 {:db/ident :tag/description
  :db/valueType :db.type/string
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
 {:db/ident :group/admins
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/many
  :db/id #db/id [:db.part/db]
  :db.install/_attribute :db.part/db}
 {:db/ident :group/settings
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
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

 ; bots
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
 {:db/ident :bot/watched
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/many
  :db/id #db/id [:db.part/db]
  :db.install/_attribute :db.part/db}

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
  :db.install/_attribute :db.part/db}

  ])
