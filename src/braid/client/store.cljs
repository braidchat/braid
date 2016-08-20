(ns braid.client.store
  (:require [cljs-utils.core :refer [flip]]
            [cljs-uuid-utils.core :as uuid]
            [taoensso.timbre :as timbre :refer-macros [errorf]]
            [clojure.set :as set]
            [schema.core :as s :include-macros true]
            [braid.common.schema :as app-schema]
            [reagent.core :as r]
            [braid.client.state.helpers :as helpers]))

(defonce app-state
  (r/atom
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
     :focused-thread-id nil}))

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

(defn transact! [v]
  (try
    (do
      (check-app-state! v)
      (reset! app-state v))
    (catch ExceptionInfo e
      (errorf "State consistency error: %s"
              (:error (ex-data e)))
      (swap! app-state helpers/display-error
             (gensym :internal-consistency)
             "Something has gone wrong"))))

; GETTERS


; used in autocomplete and group_settings

(defn id->group [group-id]
  (get-in @app-state [:groups group-id]))

; used in autocomplete and routes

(defn open-group-id []
  (get @app-state :open-group-id))

