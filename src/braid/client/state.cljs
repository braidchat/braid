(ns braid.client.state
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.store :as store]
            [clojure.set :refer [union intersection subset?]]
[braid.client.state.subscription :refer [subscription]]
            [braid.client.quests.subscriptions])
  (:import goog.Uri))

(defn subscribe
  "Get a reaction for the given data.
  In one-argument form, this looks like `(subscribe [:key arg1 arg2])`
  Two-argument form enables you to have a subscription which takes reactions or
  atoms as arguments, e.g.
  `(let [foo (subscribe [:some-key 1])
         bar (r/atom ...)
         baz (subscribe [:other-key 2] [foo bar])]
     ...)"
  ([v] (subscription store/app-state v))
  ([v dynv] ; Dynamic subscription
   (let [dyn-vals (reaction (mapv deref dynv))
         sub (reaction (subscription store/app-state (into v @dyn-vals)))]
     (reaction @@sub))))

(defmethod subscription :default
  [_ [sub-name args]]
  (ex-info (str "No subscription for " sub-name) {::name sub-name ::args args}))

(defmethod subscription :active-group
  [state _]
  (let [group-id (reaction (:open-group-id @state))]
    (reaction (get-in @state [:groups @group-id]))))

(defmethod subscription :groups
  [state _]
  (reaction (vals (:groups @state))))

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

(defmethod subscription :ordered-groups
  [state _]
  (let [groups (subscription state [:groups])
        group-order (subscription state [:user-preference :groups-order])]
    (reaction (order-groups @groups @group-order))))

(defmethod subscription :group-threads
  [state [_ group-id]]
  (reaction (->> (get-in @state [:group-threads group-id])
                 (map #(get-in @state [:threads %]))
                 doall)))

(defmethod subscription :group-admins
  [state [_ group-id]]
  (reaction (get-in @state [:groups group-id :admins])))

(defmethod subscription :group-bots
  [state [_ group-id]]
  (reaction (get-in @state [:groups group-id :bots])))

(defmethod subscription :user
  [state [_ user-id]]
  (reaction (if-let [u (get-in @state [:users user-id])]
              u
              (let [g-id (@state :open-group-id)
                    group-bots (get-in @state [:groups g-id :bots])]
                (-> group-bots
                    (->> (filter (fn [b] (= (b :user-id) user-id))))
                    first
                    (assoc :bot? true))))))

(defmethod subscription :users
  [state _]
  (reaction (@state :users)))

(defmethod subscription :user-is-group-admin?
  [state [_ user-id group-id]]
  (reaction (contains? (get-in @state [:groups group-id :admins]) user-id)))

(defmethod subscription :current-user-is-group-admin?
  [state [_ group-id]]
  (reaction (->> (get-in @state [:session :user-id])
                 (contains? (set (get-in @state [:groups group-id :admins]))))))

(defmethod subscription :open-thread-ids
  [state _]
  (reaction (get-in @state [:user :open-thread-ids])))

(defmethod subscription :group-unread-count
  [state [_ group-id]]
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
    (reaction (count @unseen-threads))))

(defmethod subscription :page
  [state _]
  (reaction (@state :page)))

(defmethod subscription :page-path
  [state _]
  (let [page (subscription state [:page])
        open-group (subscription state [:open-group-id])]
    (reaction
      ; depend on page & group, so when the page changes this sub updates too
      (do @page
          @open-group
          (.getPath (.parse Uri js/window.location))))))

(defmethod subscription :thread
  [state [_ thread-id]]
  (reaction (get-in @state [:threads thread-id])))

(defmethod subscription :threads
  [state _]
  (reaction (@state :threads)))

(defmethod subscription :page-id
  [state _]
  (reaction (get-in @state [:page :id])))

(defmethod subscription :open-threads
  [state [_ group-id]]
  (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        threads (reaction (@state :threads))
        open-threads (reaction (vals (select-keys @threads @open-thread-ids)))]
    (reaction
      (doall (filter (fn [thread] (= (thread :group-id) group-id))
                     @open-threads)))))

(defmethod subscription :recent-threads
  [state [_ group-id]]
  (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        threads (reaction (@state :threads))]
    (reaction
      (doall (filter (fn [thread] (and
                                    (= (thread :group-id) group-id)
                                    (not (contains? @open-thread-ids (thread :id)))))
                     (vals @threads))))))

(defmethod subscription :users-in-group
  [state [_ group-id]]
  (reaction
    (->> (@state :users)
         vals
         (filter (fn [u] (contains? (set (u :group-ids)) group-id)))
         doall)))

(defmethod subscription :open-group-id
  [state _]
  (reaction (get-in @state [:open-group-id])))

(defmethod subscription :users-in-open-group
  [state [_ status]]
  (reaction (->> @(subscription state state [:users-in-group (@state :open-group-id)])
                 (filter (fn [u] (= status (u :status))))
                 doall)))

(defmethod subscription :user-id
  [state _]
  (reaction (get-in @state [:session :user-id])))

(defmethod subscription :tags
  [state _]
  (reaction (vals (get-in @state [:tags]))))

(defmethod subscription :user-subscribed-to-tag?
  [state [_ tag-id]]
  (reaction (contains? (set (get-in @state [:user :subscribed-tag-ids])) tag-id)))

(defmethod subscription :group-subscribed-tags
  [state _]
  (reaction
    (into ()
          (comp
            (filter (fn [tag] @(subscription state [:user-subscribed-to-tag?  (tag :id)])))
            (filter (fn [tag] (= (get-in @state [:open-group-id]) (tag :group-id)))))
          (vals (get-in @state [:tags])))))

(defmethod subscription :user-avatar-url
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :avatar])))

(defmethod subscription :user-status
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :status])))

(defmethod subscription :search-query
  [state _]
  (reaction (get-in @state [:page :search-query])))

(defmethod subscription :tags-for-thread
  [state [_ thread-id]]
  (let [tag-ids (reaction (get-in @state [:threads thread-id :tag-ids]))
        tags (reaction (doall
                         (map (fn [thread-id]
                                (get-in @state [:tags thread-id])) @tag-ids)))]
    tags))

(defmethod subscription :mentions-for-thread
  [state [_ thread-id]]
  (let [mention-ids (reaction (get-in @state [:threads thread-id :mentioned-ids]))
        mentions (reaction (doall
                             (map (fn [user-id]
                                    (get-in @state [:users user-id])) @mention-ids)))]
    mentions))

(defmethod subscription :messages-for-thread
  [state [_ thread-id]]
  (reaction (get-in @state [:threads thread-id :messages])))

(defmethod subscription :thread-open?
  [state [_ thread-id]]
  (reaction (contains? (set (get-in @state [:user :open-thread-ids])) thread-id)))

(defmethod subscription :thread-focused?
  [state [_ thread-id]]
  (reaction (= thread-id (get-in @state [:focused-thread-id]))))

(defmethod subscription :thread-last-open-at
  [state [_ thread-id]]
  (reaction (get-in @state [:threads thread-id :last-open-at])))

(defmethod subscription :thread-new-message
  [state [_ thread-id]]
  (reaction (if-let [th (get-in @state [:threads thread-id])]
              (get th :new-message "")
              (get-in @state [:new-thread-msg thread-id] ""))))

(defmethod subscription :errors
  [state _]
  (reaction (get-in @state [:errors])))

(defmethod subscription :login-state
  [state _]
  (reaction (get-in @state [:login-state])))

(defmethod subscription :tag
  [state [_ tag-id]]
  (reaction (get-in @state [:tags tag-id])))

(defmethod subscription :group-for-tag
  [state [_ tag-id]]
  (reaction (get-in @state [:tags tag-id :group-id])))

(defmethod subscription :nickname
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :nickname])))

(defmethod subscription :invitations
  [state _]
  (reaction (get-in @state [:invitations])))

(defmethod subscription :pagination-remaining
  [state _]
  (reaction (@state :pagination-remaining)))

(defmethod subscription :user-subscribed-tag-ids
  [state _]
  (reaction (set (get-in @state [:user :subscribed-tag-ids]))))

(defmethod subscription :connected?
  [state _]
  (reaction (not-any? (fn [[k _]] (= :disconnected k)) (@state :errors))))

(defmethod subscription :new-thread-id
  [state _]
  (reaction (get @state :new-thread-id)))

(defmethod subscription :user-preference
  [state [_ pref]]
  (reaction (get-in @state [:preferences pref])))
