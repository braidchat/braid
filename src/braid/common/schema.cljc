(ns braid.common.schema
  (:require [schema.core :as s :include-macros true]
            #?(:clj [taoensso.timbre :as timbre :refer [debugf]]
               :cljs [taoensso.timbre :as timbre :refer-macros [debugf]]))
  (:import #?(:clj [clojure.lang ExceptionInfo])))


(def NewMessage
  "A new message, before saved into thread - what the client sends"
  {:id s/Uuid
   :thread-id s/Uuid
   :group-id s/Uuid
   :user-id s/Uuid
   :content s/Str
   :created-at s/Inst
   :mentioned-user-ids [s/Uuid]
   :mentioned-tag-ids [s/Uuid]
   (s/optional-key :failed?) s/Bool
   (s/optional-key :collapse?) s/Bool
   (s/optional-key :unseen?) s/Bool
   (s/optional-key :first-unseen?) s/Bool})
(def check-new-message! (s/validator NewMessage))
(defn new-message-valid? [msg]
  (try (check-new-message! msg) true
    (catch ExceptionInfo e
      (debugf "Bad message format: %s" (:error (ex-data e)))
      false)))

(def ThreadMessage
  "A message saved into a thread"
  {:id s/Uuid
   :content s/Str
   :user-id s/Uuid
   :created-at s/Inst})

(def MsgThread
  "A message thread
  Just calling it Thread apparently causes confusion with java.lang.Thread"
  {:id s/Uuid
   :group-id s/Uuid
   :messages [(s/conditional
                #(contains? % :thread-id) NewMessage
                :else ThreadMessage)]
   :tag-ids #{s/Uuid}
   :mentioned-ids #{s/Uuid}
   (s/optional-key :last-open-at) (s/cond-pre s/Int s/Inst)
   (s/optional-key :new-message) (s/maybe s/Str)})

;; Notification rules schema
(def NotifyRules
  "User notification rules"
  [(s/conditional
     #(or (= :any (first %)) (= :mention (first %)))
     [(s/one (s/enum :any :mention) "rule")
      (s/one (s/cond-pre (s/eq :any) s/Uuid) "id")]

     #(= :tag (first %))
     [(s/one (s/eq :tag) "rule") (s/one s/Uuid "id")])])
(def check-rules! (s/validator NotifyRules))
(defn rules-valid? [rs]
  (try (check-rules! rs) true
    (catch ExceptionInfo _ false)))

(def Extension
  {:id s/Uuid
   :type s/Keyword})

(def Group
  {:id s/Uuid
   :name s/Str
   :admins #{s/Uuid}
   :intro (s/maybe s/Str)
   :avatar (s/maybe s/Str)
   :extensions [Extension]
   :public? s/Bool})

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
