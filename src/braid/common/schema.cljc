(ns braid.common.schema
  (:require [schema.core :as s :include-macros true]))

(def NewMessage
  {:id s/Uuid
   :thread-id s/Uuid
   :user-id s/Uuid
   :content s/Str
   :created-at s/Inst
   :mentioned-user-ids [s/Uuid]
   :mentioned-tag-ids [s/Uuid]})

(def ThreadMessage
  {:id s/Uuid
   :content s/Str
   :user-id s/Uuid
   :created-at s/Inst})

(def MsgThread
  {:id s/Uuid
   :messages [ThreadMessage]
   :tag-ids [s/Uuid]
   :mentioned-ids [s/Uuid]})

;; Notification rules schema
(def AnyRule [(s/one (s/eq :any) "rule")
              (s/one (s/cond-pre (s/eq :any) s/Uuid) "id")])
(def MentionRule [(s/one (s/eq :mention) "rule")
                  (s/one (s/cond-pre (s/eq :any) s/Uuid) "id")])
(def TagRule [(s/one (s/eq :tag) "rule")
              (s/one s/Uuid "id")])
(def Rule
  (s/conditional #(= :any (first %)) AnyRule
                 #(= :mention (first %)) MentionRule
                 #(= :tag (first %)) TagRule))
(def Rules [Rule])
