(ns braid.core.server.schema
  (:require
   [clojure.spec.alpha :as s]
   [datomic.db]
   [spec-tools.data-spec :as ds]
   [braid.core.hooks :as hooks]))

(def -initial-schema
  [ ; user
   {:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}
   {:db/ident :user/nickname
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}
   {:db/ident :user/password-token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :user/avatar
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   ; user - thread
   {:db/ident :user/open-thread
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :user/subscribed-thread
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   ; user - tag
   {:db/ident :user/subscribed-tag
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   ; user - preferences
   {:db/ident :user/preferences
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   ; user preference - key
   {:db/ident :user.preference/key
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   ; user preference - value
   {:db/ident :user.preference/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   ; message
   {:db/ident :message/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :message/content
    :db/valueType :db.type/string
    :db/fulltext true
    :db/cardinality :db.cardinality/one}
   {:db/ident :message/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   ; message - user
   {:db/ident :message/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   ; message - thread
   {:db/ident :message/thread
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   ; thread
   {:db/ident :thread/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   ; thread - tag
   {:db/ident :thread/tag
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   ; thread - mentions
   {:db/ident :thread/mentioned
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   ; thread - group
   {:db/ident :thread/group
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   ; tag
   {:db/ident :tag/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :tag/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :tag/group
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :tag/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   ; groups
   {:db/ident :group/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :group/slug
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}
   {:db/ident :group/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :group/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :group/admins
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :group/settings
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   ; invitations
   {:db/ident :invite/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :invite/group
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :invite/from
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :invite/to
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :invite/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])

(def rule-dataspec
  {:db/ident keyword?
   (ds/opt :db/doc) string?
   :db/valueType (s/spec #{:db.type/boolean
                           :db.type/instant
                           :db.type/keyword
                           :db.type/long
                           :db.type/ref
                           :db.type/string
                           :db.type/uuid})
   :db/cardinality (s/spec #{:db.cardinality/one
                             :db.cardinality/many})
   (ds/opt :db/unique) (s/spec #{:db.unique/identity
                                 :db.unique/value})})

(defonce schema
  (hooks/register! (atom -initial-schema) [rule-dataspec]))
