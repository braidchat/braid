(ns braid.core.client.store
  (:require
   [braid.core.client.invites.schema :as invites]
   [braid.core.common.schema :as app-schema]
   [schema.core :as s :include-macros true]))

(def AppState
  (merge
    {:login-state (s/enum :auth-check :login-form :ws-connect
                          :app :gateway)
     :websocket-state {:connected? s/Bool
                       :next-reconnect (s/maybe s/Int)}
     :csrf-token s/Str
     :action s/Keyword
     :user-auth {:user s/Any
                 :error s/Any
                 :checking? s/Bool
                 :mode (s/enum :register :log-in :request-password-reset)
                 :should-validate? s/Bool
                 :oauth-provider s/Any
                 :validations s/Any
                 :fields s/Any}
     :open-group-id (s/maybe s/Uuid)
     :threads {s/Uuid app-schema/MsgThread}
     :group-threads {s/Uuid #{s/Uuid}}
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
     :preferences {s/Keyword s/Any}
     :notifications {:window-visible? s/Bool
                     :unread-count s/Int}
     :user {:open-thread-ids #{s/Uuid}
            :subscribed-tag-ids #{s/Uuid}}
     :focused-thread-id (s/maybe s/Uuid)
     :temp-threads {s/Uuid {:id s/Uuid
                            :new? s/Bool
                            :messages s/Any
                            :group-id s/Uuid
                            :mentioned-ids [s/Uuid]
                            :tag-ids [s/Uuid]
                            :new-message s/Str}}
     :action-disabled? s/Bool
     (s/optional-key :public-groups) #{app-schema/Group}}
    invites/InvitesAppState))

(def initial-state
  (merge
    {:login-state :gateway ; :ws-connect :app
     :websocket-state {:connected? false
                       :next-reconnect nil}
     :open-group-id nil
     :threads {}
     :group-threads {}
     :tags {}
     :groups {}
     :page {:type nil}
     :session nil
     :errors []
     :preferences {}
     :notifications {:window-visible? true
                     :unread-count 0}
     :user {:open-thread-ids #{}
            :subscribed-tag-ids #{}}
     :focused-thread-id nil
     :temp-threads {}}
    invites/init-state))
