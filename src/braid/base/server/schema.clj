(ns braid.base.server.schema
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]
   [braid.core.hooks :as hooks]))

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
  (hooks/register! (atom []) [rule-dataspec]))
