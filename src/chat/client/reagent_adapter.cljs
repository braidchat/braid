(ns chat.client.reagent-adapter
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [chat.client.store :as store]
            [braid.common.state :as state]))

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
  :group-for-tag
  state/get-group-for-tag)

(register-sub
  :open-thread-ids
  state/get-open-thread-ids)

(register-sub
  :get-threads-for-group
  state/get-threads-for-group)

(register-sub
  :nickname
  state/get-nickname)

(register-sub
  :invitations
  state/get-invitations)

(register-sub
  :users
  state/get-users)


(defn subscribe [v]
  (let [key-v (first v)
        handler-fn (get @subscriptions key-v)]
    (handler-fn store/app-state v)))


