(ns chat.client.reagent-adapter
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [chat.client.store :as store]
            [braid.client.state :as state]))

(defn reagent->react [component]
  (js/React.createFactory
    (r/reactify-component
      component)))

(defonce subscriptions (atom {}))

(defn register-sub [key-v handler-fn]
  (swap! subscriptions assoc key-v handler-fn))

(register-sub
  :group-unread-count
  state/get-group-unread-count)

(register-sub
  :groups
  state/get-groups)

(register-sub
  :group-admins
  state/get-group-admins)

(register-sub
  :group-threads
  state/get-group-threads)

(register-sub
  :group-bots
  state/get-group-bots)

(register-sub
  :recent-threads
  state/get-recent-threads)

(register-sub
  :user-is-group-admin?
  state/user-is-group-admin?)

(register-sub
  :current-user-is-group-admin?
  state/current-user-is-group-admin?)

(register-sub
  :active-group
  state/get-active-group)

(register-sub
  :page
  state/get-page)

(register-sub
  :page-id
  state/get-page-id)

(register-sub
  :open-threads
  state/get-open-threads)

(register-sub
  :users-in-open-group
  state/get-users-in-open-group)

(register-sub
  :user-id
  state/get-user-id)

(register-sub
  :tag
  state/get-tag)

(register-sub
  :tags
  state/get-all-tags)

(register-sub
  :group-subscribed-tags
  state/get-group-subscribed-tags)

(register-sub
  :user-subscribed-to-tag
  state/get-user-subscribed-to-tag)

(register-sub
  :user-avatar-url
  state/get-user-avatar-url)

(register-sub
  :user-status
  state/get-user-status)

(register-sub
  :open-group-id
  state/get-open-group-id)

(register-sub
  :search-query
  state/get-search-query)

(register-sub
  :tags-for-thread
  state/get-tags-for-thread)

(register-sub
  :mentions-for-thread
  state/get-mentions-for-thread)

(register-sub
  :thread-open?
  state/get-thread-open?)

(register-sub
  :thread-new-message
  state/get-thread-new-message)

(register-sub
  :errors
  state/get-errors)

(register-sub
  :login-state
  state/get-login-state)

(register-sub
  :group-for-tag
  state/get-group-for-tag)

(register-sub
  :open-thread-ids
  state/get-open-thread-ids)

(register-sub
  :nickname
  state/get-nickname)

(register-sub
  :invitations
  state/get-invitations)

(register-sub
  :users
  state/get-users)

(register-sub
  :user
  state/get-user)

(register-sub
  :thread
  state/get-thread)

(register-sub
  :threads
  state/get-threads)

(register-sub
  :pagination-remaining
  state/get-pagination-remaining)

(register-sub
  :user-subscribed-tag-ids
  state/get-user-subscribed-tag-ids)

(register-sub
  :connected?
  state/get-connected?)

(register-sub
  :messages-for-thread
  state/get-messages-for-thread)

(register-sub
  :new-thread-id
  state/get-new-thread-id)

(register-sub
  :user-preference
  state/get-preference)

(defn subscribe
  ([v]
   (let [key-v (first v)
         handler-fn (get @subscriptions key-v)]
     (handler-fn store/app-state v)))
  ([v dynv] ; Dynamic subscription
   (let [key-v (first v)
         handler-fn (get @subscriptions key-v)]
     (let [dyn-vals (reaction (mapv deref dynv))
           sub (reaction (handler-fn store/app-state v @dyn-vals))]
       (reaction @@sub)))))
