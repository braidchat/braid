(ns braid.client.state
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [re-frame.core :as re-frame :refer [reg-sub reg-sub-raw]])
  (:import goog.Uri))

(def subscribe re-frame/subscribe)

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

(defn order-groups
  "Helper function to impose an order on groups.
  This is a seperate function (instead of inline in :ordered-groups because the
  index route needs to be able to call this to find the first group"
  [groups group-order]
  (if (nil? group-order)
    groups
    (let [ordered? (comp boolean (set group-order) :id)
          {ord true unord false} (group-by ordered? groups)
          by-id (group-by :id groups)]
      (concat
        (map (comp first by-id) group-order)
        unord))))

(reg-sub
  :ordered-groups
  :<- [:groups]
  :<- [:user-preference :groups-order]
  (fn [[groups group-order] _]
    (order-groups groups group-order)))

(reg-sub-raw
  :group-bots
  (fn [state _ [group-id]]
    (reaction (get-in @state [:groups group-id :bots]))))

(reg-sub-raw
  :user
  (fn [state [_ q-user-id] [d-user-id]]
    (let [user-id (or d-user-id q-user-id)]
      (reaction (if-let [u (get-in @state [:users user-id])]
                  u
                  (let [g-id (@state :open-group-id)
                        group-bots (get-in @state [:groups g-id :bots])]
                    (-> group-bots
                        (->> (filter (fn [b] (= (b :user-id) user-id))))
                        first
                        (assoc :bot? true))))))))

(reg-sub-raw
  :users
  (fn [state _]
    (reaction (@state :users))))

(reg-sub-raw
  :user-is-group-admin?
  (fn [state _ [user-id group-id]]
    (reaction (contains? (get-in @state [:groups group-id :admins]) user-id))))

(reg-sub-raw
  :current-user-is-group-admin?
  (fn [state _ [group-id]]
    (reaction (->> (get-in @state [:session :user-id])
                   (contains? (set (get-in @state [:groups group-id :admins])))))))

(reg-sub-raw
  :open-thread-ids
  (fn [state _]
    (reaction (get-in @state [:user :open-thread-ids]))))

(reg-sub-raw
  :group-unread-count
  (fn [state [_ group-id]]
    (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
          threads (reaction (@state :threads))
          tags (reaction (@state :tags))
          users (reaction (@state :users))
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

(reg-sub-raw
  :page
  (fn [state _]
    (reaction (@state :page))))

(reg-sub
  :page-path
  :<- [:page]
  :<- [:open-group-id]
  (fn [[page open-group] _]
    ; depend on page & group, so when the page changes this sub updates too
    (.getPath (.parse Uri js/window.location))))

(reg-sub-raw
  :thread
  (fn [state _ [thread-id]]
    (reaction (get-in @state [:threads thread-id]))))

(reg-sub-raw
  :threads
  (fn [state _]
    (reaction (@state :threads))))

(reg-sub-raw
  :page-id
  (fn [state _]
    (reaction (get-in @state [:page :id]))))

(reg-sub-raw
  :open-threads
  (fn [state _ [group-id]]
    (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
          threads (reaction (@state :threads))
          open-threads (reaction (vals (select-keys @threads @open-thread-ids)))]
      (reaction
        (doall (filter (fn [thread] (= (thread :group-id) group-id))
                       @open-threads))))))

(reg-sub-raw
  :recent-threads
  (fn [state _ [group-id]]
    (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
          threads (reaction (@state :threads))]
      (reaction
        (doall (filter (fn [thread] (and
                                      (= (thread :group-id) group-id)
                                      (not (contains? @open-thread-ids (thread :id)))))
                       (vals @threads)))))))

(reg-sub-raw
  :users-in-group
  (fn [state [_ group-id]]
    (reaction
      (->> (@state :users)
           vals
           (filter (fn [u] (contains? (set (u :group-ids)) group-id)))
           doall))))

(reg-sub-raw
  :users-in-open-group
  (fn [state [_ status]]
    (reaction (->> @(subscribe [:users-in-group (@state :open-group-id)])
                   (filter (fn [u] (= status (u :status))))
                   doall))))

(reg-sub-raw
  :user-id
  (fn [state _]
    (reaction (get-in @state [:session :user-id]))))

(reg-sub-raw
  :tags
  (fn [state _]
    (reaction (vals (get-in @state [:tags])))))

(reg-sub-raw
  :user-subscribed-to-tag?
  (fn [state [_ q-tag-id] [d-tag-id]]
    (let [tag-id (or d-tag-id q-tag-id)]
      (reaction (contains? (set (get-in @state [:user :subscribed-tag-ids])) tag-id)))))

(reg-sub-raw
  :group-subscribed-tags
  (fn [state _]
    (reaction
      (into ()
            (comp
              (filter (fn [tag] @(subscribe [:user-subscribed-to-tag? (tag :id)])))
              (filter (fn [tag] (= (get-in @state [:open-group-id]) (tag :group-id)))))
            (vals (get-in @state [:tags]))))))

(reg-sub-raw
  :user-avatar-url
  (fn [state _ [user-id]]
    (reaction (get-in @state [:users user-id :avatar]))))

(reg-sub-raw
  :user-status
  (fn [state _ [user-id]]
    (reaction (get-in @state [:users user-id :status]))))

(reg-sub-raw
  :search-query
  (fn [state _]
    (reaction (get-in @state [:page :search-query]))))

(reg-sub-raw
  :tags-for-thread
  (fn [state _ [thread-id]]
    (let [tag-ids (reaction (get-in @state [:threads thread-id :tag-ids]))
          tags (reaction (doall
                           (map (fn [thread-id]
                                  (get-in @state [:tags thread-id])) @tag-ids)))]
      tags)))

(reg-sub-raw
  :mentions-for-thread
  (fn [state _ [thread-id]]
    (let [mention-ids (reaction (get-in @state [:threads thread-id :mentioned-ids]))
          mentions (reaction (doall
                               (map (fn [user-id]
                                      (get-in @state [:users user-id]))
                                    @mention-ids)))]
      mentions)))

(reg-sub-raw
  :messages-for-thread
  (fn [state [_ thread-id]]
    (reaction (get-in @state [:threads thread-id :messages]))))

(reg-sub-raw
  :thread-open?
  (fn [state [_ thread-id]]
    (reaction (contains? (set (get-in @state [:user :open-thread-ids])) thread-id))))

(reg-sub-raw
  :thread-focused?
  (fn [state [_ thread-id]]
    (reaction (= thread-id (get-in @state [:focused-thread-id])))))

(reg-sub-raw
  :thread-last-open-at
  (fn [state [_ thread-id]]
    (reaction (get-in @state [:threads thread-id :last-open-at]))))

(reg-sub-raw
  :thread-new-message
  (fn [state _ [thread-id]]
    (reaction (if-let [th (get-in @state [:threads thread-id])]
                (get th :new-message "")
                (get-in @state [:new-thread-msg thread-id] "")))))

(reg-sub-raw
  :errors
  (fn [state _]
    (reaction (get-in @state [:errors]))))

(reg-sub-raw
  :login-state
  (fn [state _]
    (reaction (get-in @state [:login-state]))))

(reg-sub
  :tag
  (fn [state _ [tag-id]]
    (get-in state [:tags tag-id])))

(reg-sub-raw
  :nickname
  (fn [state _ [user-id]]
    (reaction (get-in @state [:users user-id :nickname]))))

(reg-sub-raw
  :invitations
  (fn [state _]
    (reaction (get-in @state [:invitations]))))

(reg-sub-raw
  :pagination-remaining
  (fn [state _]
    (reaction (@state :pagination-remaining))))

(reg-sub-raw
  :user-subscribed-tag-ids
  (fn [state _]
    (reaction (set (get-in @state [:user :subscribed-tag-ids])))))

(reg-sub-raw
  :connected?
  (fn [state _]
    (reaction (not-any? (fn [[k _]] (= :disconnected k)) (@state :errors)))))

(reg-sub-raw
  :new-thread-id
  (fn [state _]
    (reaction (get @state :new-thread-id))))

(reg-sub-raw
  :user-preference
  (fn [state [_ pref]]
    (reaction (get-in @state [:preferences pref]))))

(reg-sub
  :tags-in-group
  (fn [state [_ group-id]]
    (->> (state :tags)
         vals
         (filter #(= group-id (:group-id %)))
         doall)))
