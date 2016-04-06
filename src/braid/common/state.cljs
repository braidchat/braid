(ns braid.common.state
  (:require
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [clojure.set :refer [intersection]]))

(defn set-active-group-id!
  [state [_ group-id]]
  (assoc state :open-group-id group-id))

(defn get-active-group
  [state _]
  (let [group-id (reaction (:open-group-id @state))]
    (reaction (get-in @state [:groups @group-id]))))

(defn- unseen? [message thread]
  (> (:created-at message)
     (thread :last-open-at)))

(defn get-groups-with-unread
  [state _]
  (let [groups (reaction (vals (:groups @state)))
        group-id->unread-count
        (->>
          (select-keys (@state :threads)
                       (get-in @state [:user :open-thread-ids]))
          vals
          (filter (fn [thread]
                    (unseen? (->> (thread :messages)
                                  (sort-by :created-at)
                                  last)
                             thread)))
          (mapcat (fn [thread]
                    (let [group-ids-from-tags
                          (->> (thread :tag-ids)
                               (map (fn [tag-id]
                                      (get-in @state [:tags tag-id :group-id])))
                               set)]
                      (if (seq group-ids-from-tags)
                        group-ids-from-tags
                        (let [group-ids-from-users
                              (->> (thread :messages)
                                   (map :user-id)
                                   set
                                   (map (fn [user-id]
                                          (set (get-in @state [:users user-id :group-ids]))))
                                   (apply intersection))]
                          group-ids-from-users)))))
          frequencies)]
    (reaction (map (fn [group]
                     (assoc group :unread-count (group-id->unread-count (group :id))))
                   @groups))))

(defn get-page
  [state _]
  (reaction (@state :page)))
