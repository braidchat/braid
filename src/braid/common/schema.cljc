(ns braid.common.schema
  (:require [schema.core :as s :include-macros true]))


(def NewMessage
  "A new message, before saved into thread - what the client sends"
  {:id s/Uuid
   :thread-id s/Uuid
   :user-id s/Uuid
   :content s/Str
   :created-at s/Inst
   :mentioned-user-ids [s/Uuid]
   :mentioned-tag-ids [s/Uuid]})
(def new-message-valid? (s/validator NewMessage))

(def ThreadMessage
  "A message saved into a thread"
  {:id s/Uuid
   :content s/Str
   :user-id s/Uuid
   :created-at s/Inst})

(def MsgThread
  "A thread (just calling it Thread apparently causes confusion (with java.lang.Thread)"
  {:id s/Uuid
   :messages [ThreadMessage]
   :tag-ids #{s/Uuid}
   :mentioned-ids #{s/Uuid}
   (s/optional-key :last-open-at) (s/cond-pre s/Int s/Inst)})

;; Notification rules schema
(def NotifyRule
  "One rule for user notification settings"
  (s/conditional
    #(or (= :any (first %)) (= :mention (first %)))
    [(s/one (s/enum :any :mention) "rule")
     (s/one (s/cond-pre (s/eq :any) s/Uuid) "id")]

    #(= :tag (first %))
    [(s/one (s/eq :tag) "rule") (s/one s/Uuid "id")]))
(def NotifyRules "User notification rules" [NotifyRule])
(def rules-valid? (s/validator NotifyRules))

(def Extension
  {:id s/Uuid
   :type s/Keyword})

(def Group
  {:id s/Uuid
   :name s/Str
   :admins #{s/Uuid}
   :intro (s/maybe s/Str)
   :avatar (s/maybe s/Str)
   :extensions [Extension]})

(def User
  {:id s/Uuid
   :nickname s/Str
   :avatar s/Str
   :group-ids [s/Uuid]
   (s/optional-key :status) (s/enum :online :offline)})

(def Tag
  {:id s/Uuid
   :name s/Str
   :description (s/maybe s/Str)
   :group-id s/Uuid
   :group-name s/Str
   :threads-count s/Int
   :subscribers-count s/Int})

(def Invitation
  {:id s/Uuid
   :inviter-id s/Uuid
   :inviter-email s/Str
   :inviter-nickname s/Str
   :invitee-email s/Str
   :group-id s/Uuid
   :group-name s/Str})
