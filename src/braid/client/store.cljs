(ns braid.client.store
  (:require [cljs-uuid-utils.core :as uuid]
            [schema.core :as s :include-macros true]
            [braid.common.schema :as app-schema]))

(def initial-state
  {:login-state :auth-check ; :ws-connect :login-form :app
   :open-group-id nil
   :threads {}
   :group-threads {}
   :new-thread-msg {}
   :pagination-remaining 0
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
   :new-thread-id (uuid/make-random-squuid)
   :focused-thread-id nil})

(def AppState
  {:login-state (s/enum :auth-check :login-form :ws-connect :app)
   :open-group-id (s/maybe s/Uuid)
   :threads {s/Uuid app-schema/MsgThread}
   :group-threads {s/Uuid #{s/Uuid}}
   :new-thread-msg {s/Uuid s/Str}
   :pagination-remaining s/Int
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
   :new-thread-id s/Uuid
   :focused-thread-id (s/maybe s/Uuid)})

(def check-app-state! (s/validator AppState))
