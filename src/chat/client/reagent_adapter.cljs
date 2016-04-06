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
  :groups-with-unread
  state/get-groups-with-unread)

(register-sub
  :active-group
  state/get-active-group)

(register-sub
  :page
  state/get-page)

(defn subscribe [v]
  (let [key-v (first v)
        handler-fn (get @subscriptions key-v)]
    (handler-fn store/app-state v)))


