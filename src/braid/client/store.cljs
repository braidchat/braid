(ns braid.client.store
  (:require [schema.core :as s :include-macros true]
            [braid.common.schema :as app-schema]
            [braid.client.quests.state :as quests]))

(def initial-state
  (merge
    {:login-state :auth-check ; :ws-connect :login-form :app
     :open-group-id nil
     :threads {}
     :group-threads {}
     :users {}
     :tags {}
     :groups {}
     :page {:type :inbox}
     :session nil
     :errors []
     :invitations []
     :preferences {}
     :notifications {:window-visible? true
                     :unread-count 0}
     :user {:open-thread-ids #{}
            :subscribed-tag-ids #{}}
     :focused-thread-id nil
     :temp-threads {}}
    quests/init-state))

(def AppState
  (merge
    {:login-state (s/enum :auth-check :login-form :ws-connect :app)
     :open-group-id (s/maybe s/Uuid)
     :threads {s/Uuid app-schema/MsgThread}
     :group-threads {s/Uuid #{s/Uuid}}
     :users {s/Uuid app-schema/User}
     :tags {s/Uuid app-schema/Tag}
     :groups {s/Uuid app-schema/Group}
     :page {:type s/Keyword
            (s/optional-key :id) s/Uuid
            (s/optional-key :thread-ids) [s/Uuid]
            (s/optional-key :search-query) s/Str
            (s/optional-key :loading?) s/Bool
            (s/optional-key :error?) s/Bool}
     :session (s/maybe {:user-id s/Uuid})
     :errors [[(s/one (s/cond-pre s/Keyword s/Str) "err-key") (s/one s/Str "msg")
               (s/one (s/enum :error :warn :info) "type")]]
     :invitations [app-schema/Invitation]
     :preferences {s/Keyword s/Any}
     :notifications {:window-visible? s/Bool
                     :unread-count s/Int}
     :user {:open-thread-ids #{s/Uuid}
            :subscribed-tag-ids #{s/Uuid}}
     :focused-thread-id (s/maybe s/Uuid)
     :temp-threads {s/Uuid {:id s/Uuid
                            :tag-ids [s/Uuid]
                            :new-message s/Str}}}
    quests/QuestsState))

(def check-app-state! (s/validator AppState))
