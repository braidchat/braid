(ns braid.core.common.schema
  (:require
    [clojure.spec.alpha :as s]
    [spec-tools.data-spec :as ds]))

(def NewMessage
  "A new message, before saved into thread - what the client sends"
  {:id uuid?
   :thread-id uuid?
   :group-id uuid?
   :user-id uuid?
   :content string?
   :created-at inst?
   :mentioned-user-ids [uuid?]
   :mentioned-tag-ids [uuid?]
   (ds/opt :failed?) boolean?
   (ds/opt :collapse?) boolean?
   (ds/opt :unseen?) boolean?
   (ds/opt :first-unseen?) boolean?})

(def ThreadMessage
  "A message saved into a thread"
  {:id uuid?
   :content string?
   :user-id uuid?
   :created-at inst?})

(def MsgThread
  "A message thread
  Just calling it Thread apparently causes confusion with java.lang.Thread"
  {:id uuid?
   :group-id uuid?
   :messages [(ds/or {:new-messages NewMessage
                      :messages ThreadMessage}) ]
   :tag-ids #{uuid?}
   :mentioned-ids #{uuid?}
   (ds/opt :last-open-at) (ds/or {:integer integer?
                                  :inst inst?})
   (ds/opt :new-message) (ds/maybe string?)})

(defn NotifyRule
  "User notification rules"
  [[rule id]]
  (and
    (contains? #{:any :mention :tag} rule)
    (or (and (= :tag rule) (uuid? id))
        (and (= :any rule) (or (uuid? id)
                               (= :any id)))
        (and (= :mention rule) (or (uuid? id)
                                   (= :any id))))))

(def NotifyRules
  [NotifyRule])

(def Bot
  {:id uuid?
   :group-id uuid?
   :user-id uuid?
   :name string?
   :avatar string?
   :webhook-url string?
   :event-webhook-url (ds/maybe string?)
   :token string?
   :notify-all-messages? boolean?})

(def BotDisplay
  "Like Bot but for publicly-available bot info"
  {:id uuid?
   :user-id uuid?
   :nickname string?
   :avatar string?})

(def UserId
  uuid?)

(def User
  {:id UserId
   :nickname string?
   :avatar string?
   (ds/opt :email) (ds/maybe string?)
   (ds/opt :status) (s/spec #{:online :offline})})

(def Group
  {:id uuid?
   :name string?
   :slug string?
   :admins #{uuid?}
   :intro (ds/maybe string?)
   :avatar (ds/maybe string?)
   :public? boolean?
   :users-count integer?
   :users {UserId User}
   :bots #{BotDisplay}})

(def Tag
  {:id uuid?
   :name string?
   :description (ds/maybe string?)
   :group-id uuid?
   :threads-count integer?
   :subscribers-count integer?})

(def Invitation
  {:id uuid?
   :inviter-id uuid?
   :inviter-email string?
   :inviter-nickname string?
   :invitee-email string?
   :group-id uuid?
   :group-name string?})
