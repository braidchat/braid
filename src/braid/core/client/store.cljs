(ns braid.core.client.store
  (:require
   [braid.core.client.invites.schema :as invites]
   [braid.core.common.schema :as app-schema]
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]))

(def AppState
  (merge
    {:login-state (s/spec #{:auth-check :login-form :ws-connect
                            :app :gateway :anon-connected :anon-ws-connect})
     :websocket-state {:connected? boolean?
                       :next-reconnect (ds/maybe integer?)}
     :csrf-token (ds/maybe string?)
     :action (ds/maybe keyword?)
     :user-auth (ds/maybe
                  {:user any?
                   :error any?
                   :checking? boolean?
                   :mode (s/spec #{:register :log-in :request-password-reset})
                   :should-validate? boolean?
                   :oauth-provider any?
                   :validations any?
                   :fields any?})
     :open-group-id (ds/maybe uuid?)
     :threads {uuid? app-schema/MsgThread}
     :group-threads {uuid? #{uuid?}}
     :tags {uuid? app-schema/Tag}
     :groups {uuid? app-schema/Group}
     :page {:type (ds/maybe keyword?)
            (ds/opt :id) uuid?
            (ds/opt :loading?) boolean?
            (ds/opt :error?) boolean?}
     :session (ds/maybe {:user-id uuid?})
     :preferences {keyword? any?}
     :notifications {:window-visible? boolean?
                     :unread-count integer?}
     :user {:open-thread-ids #{uuid?}
            :subscribed-tag-ids #{uuid?}}
     :focused-thread-id (ds/maybe uuid?)
     :temp-threads {uuid? {:id uuid?
                           :new? boolean?
                           :messages any?
                           :group-id uuid?
                           :mentioned-ids [uuid?]
                           :tag-ids [uuid?]
                           :new-message string?}}
     :action-disabled? boolean?}
    invites/InvitesAppState))

(def initial-state
  (merge
    {:login-state :gateway ; :ws-connect :app
     :websocket-state {:connected? false
                       :next-reconnect nil}
     :action nil
     :action-disabled? false
     :csrf-token nil
     :user-auth nil
     :open-group-id nil
     :threads {}
     :group-threads {}
     :tags {}
     :groups {}
     :page {:type nil}
     :session nil
     :preferences {}
     :notifications {:window-visible? true
                     :unread-count 0}
     :user {:open-thread-ids #{}
            :subscribed-tag-ids #{}}
     :focused-thread-id nil
     :temp-threads {}}
    invites/init-state))
