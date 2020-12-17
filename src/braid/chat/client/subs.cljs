(ns braid.chat.client.subs
  (:require
   [braid.core.client.state.helpers :refer [order-groups]]
   [re-frame.core :refer [subscribe reg-sub reg-sub-raw]]
   [reagent.ratom :include-macros true :refer-macros [reaction]])
  (:import
   (goog Uri)))

(reg-sub
  :open-group-id
  (fn [state _]
    (get-in state [:open-group-id])))

(reg-sub-raw
  :active-group
  (fn [state _]
    (let [group-id (reaction (:open-group-id @state))]
      (reaction (get-in @state [:groups @group-id])))))

(reg-sub
  :groups
  (fn [state _]
    (vals (:groups state))))

(reg-sub
  :group
  (fn [state [_ q-group-id] [d-group-id]]
    (get-in state [:groups (or d-group-id q-group-id)])))

(reg-sub
  :ordered-groups
  :<- [:groups]
  :<- [:user-preference :groups-order]
  (fn [[groups group-order] _]
    (order-groups groups group-order)))

(reg-sub
  :user
  (fn [{:keys [open-group-id] :as state} [_ q-user-id] [d-user-id]]
    (let [user-id (or d-user-id q-user-id)]
      (when-let [u (get-in state [:groups open-group-id :users user-id])]
        u))))

(reg-sub
  :users
  (fn [{:keys [open-group-id] :as state} _]
    (vals (get-in state [:groups open-group-id :users]))))

(reg-sub
  :user-is-group-admin?
  (fn [state [_ user-id] [group-id]]
    (contains? (set (get-in state [:groups group-id :admins])) user-id)))

(reg-sub
  :current-user-is-group-admin?
  (fn [state _ [group-id]]
    (->> (get-in state [:session :user-id])
         (contains? (set (get-in state [:groups group-id :admins]))))))

(reg-sub
  :open-thread-ids
  (fn [state _]
    (get-in state [:user :open-thread-ids])))

(reg-sub-raw
  :group-unread-count
  (fn [state [_ group-id]]
    (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
          threads (reaction (@state :threads))
          tags (reaction (@state :tags))
          users (reaction (get-in @state [:groups group-id :users]))
          thread-in-group? (fn [thread] (= group-id (thread :group-id)))
          thread-unseen? (fn [thread] (> (->> (thread :messages)
                                              (map :created-at)
                                              (apply max))
                                         (thread :last-open-at)))
          unseen-threads (reaction
                           (->>
                             (select-keys @threads @open-thread-ids)
                             vals
                             (filter thread-in-group?)
                             (filter thread-unseen?)))]
      (reaction (count @unseen-threads)))))

(reg-sub
  :page
  (fn [state _]
    (state :page)))

(reg-sub
  :page-path
  :<- [:page]
  :<- [:open-group-id]
  (fn [[page open-group] _]
    ; depend on page & group, so when the page changes this sub updates too
    (.getPath (.parse Uri js/window.location))))

(reg-sub
  :thread
  (fn [state [_ thread-id]]
    (get-in state [:threads thread-id])))

(reg-sub
  :thread*
  (fn [state _ [thread-id]]
    (get-in state [:threads thread-id])))

(reg-sub
  :threads
  (fn [state _]
    (state :threads)))

(reg-sub
  :page-id
  (fn [state _]
    (get-in state [:page :id])))

(reg-sub-raw
  :open-threads
  (fn [state _ [group-id]]
    (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
          threads (reaction (@state :threads))
          open-threads (reaction (vals (select-keys @threads @open-thread-ids)))]
      (reaction
        (doall (filter (fn [thread] (= (thread :group-id) group-id))
                       @open-threads))))))

(reg-sub
  :users-in-group
  (fn [state [_ group-id]]
    (vals (get-in state [:groups group-id :users]))))

(reg-sub-raw
  :users-in-open-group
  (fn [state [_ status]]
    (reaction (->> @(subscribe [:users-in-group (@state :open-group-id)])
                   (filter (fn [u] (= status (u :status))))
                   doall))))

(reg-sub
  :user-id
  (fn [state _]
    (get-in state [:session :user-id])))

(reg-sub
  :tags
  (fn [state _]
    (vals (get-in state [:tags]))))

(reg-sub
  :user-subscribed-to-tag?
  (fn [state [_ q-tag-id] [d-tag-id]]
    (let [tag-id (or d-tag-id q-tag-id)]
      (contains? (set (get-in state [:user :subscribed-tag-ids])) tag-id))))

(reg-sub-raw
  :group-subscribed-tags
  (fn [state _]
    (reaction
      (into ()
            (comp
              (filter (fn [tag] @(subscribe [:user-subscribed-to-tag? (tag :id)])))
              (filter (fn [tag] (= (get-in @state [:open-group-id]) (tag :group-id)))))
            (vals (get-in @state [:tags]))))))

(reg-sub
  :user-avatar-url
  (fn [state _ [user-id]]
    (get-in state [:groups (:open-group-id state) :users user-id :avatar])))

(reg-sub
  :messages-for-thread
  (fn [state [_ thread-id]]
    (get-in state [:threads thread-id :messages])))

(reg-sub
  :thread-open?
  (fn [state [_ thread-id]]
    (contains? (set (get-in state [:user :open-thread-ids])) thread-id)))

(reg-sub
  :thread-focused?
  (fn [state [_ thread-id]]
    (= thread-id (get-in state [:focused-thread-id]))))

(reg-sub
  :thread-last-open-at
  (fn [state [_ thread-id]]
    (get-in state [:threads thread-id :last-open-at])))

(reg-sub
  :login-state
  (fn [state _]
    (get-in state [:login-state])))

(reg-sub
  :tag
  (fn [state [_ q-tag-id] [d-tag-id]]
    (get-in state [:tags (or d-tag-id q-tag-id)])))

(reg-sub
  :nickname
  (fn [state _ [user-id]]
    (get-in state [:groups (:open-group-id state) :users user-id :nickname])))

(reg-sub
  :user-subscribed-tag-ids
  (fn [state _]
    (set (get-in state [:user :subscribed-tag-ids]))))

(reg-sub
  :connected?
  (fn [state _]
    (get-in state [:websocket-state :connected?])))

(reg-sub
  :temp-thread
  (fn [state _]
    (get-in state [:temp-threads (state :open-group-id)])))

(reg-sub
  :user-preference
  (fn [state [_ pref]]
    (get-in state [:preferences pref])))

(reg-sub
  :tags-in-group
  (fn [state [_ group-id]]
    (->> (state :tags)
         vals
         (filter #(= group-id (:group-id %)))
         doall)))

(reg-sub
  :open-group-tags
  (fn [state _]
    (->> (state :tags)
         vals
         (filter (fn [tag] (= (get-in state [:open-group-id]) (tag :group-id)))))))

(reg-sub
  :core/websocket-state
  (fn [state _]
    (state :websocket-state)))
