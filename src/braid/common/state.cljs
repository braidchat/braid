(ns braid.common.state
  (:require
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [clojure.set :refer [union intersection]]))

(defn set-active-group-id!
  [state [_ group-id]]
  (assoc state :open-group-id group-id))

(defn get-active-group
  [state _]
  (let [group-id (reaction (:open-group-id @state))]
    (reaction (get-in @state [:groups @group-id]))))

(defn get-groups
  [state _]
  (reaction (vals (:groups @state))))

(defn- thread-unseen?
  [thread]
  (> (->> (thread :messages)
          (map :created-at)
          (apply max))
     (thread :last-open-at)))

(defn get-group-unread-count
  [state [_ group-id]]
  (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        threads (reaction (@state :threads))
        tags (reaction (@state :tags))
        users (reaction (@state :users))
        group-ids->user-ids (reaction (->> @users
                                           vals
                                           (mapcat (fn [u]
                                                     (map
                                                       (fn [gid]
                                                         {:id (u :id) :group-id gid})
                                                       (u :group-ids))))
                                           (group-by :group-id)
                                           (map (fn [[k vs]]
                                                  [k (map (fn [v] (v :id)) vs)]))
                                           (into {})))
        group-user-ids (set (@group-ids->user-ids group-id))
        thread-in-group? (fn [thread]
                           (if (seq (thread :tag-ids))
                             (= group-id (:group-id (@tags (first (thread :tag-ids)))))
                             (let [user-ids-from-messages (->> (thread :messages)
                                                               (map :user-id)
                                                               set)
                                   user-ids-from-refs (set (thread :user-ids))
                                   user-ids (union user-ids-from-messages
                                                   user-ids-from-refs)]
                               (< 0 (count (intersection group-user-ids user-ids))))))
        unseen-threads (reaction
                         (->>
                           (select-keys @threads @open-thread-ids)
                           vals
                           (filter thread-unseen?)
                           (filter thread-in-group?)))]
    (reaction (count @unseen-threads))))

(defn get-page
  [state _]
  (reaction (@state :page)))

(defn get-open-threads
  [state _]
  (let [current-group-id (reaction (@state :open-group-id))
        open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        group-for-tag (fn [tag-id]
                        (get-in @state [:tags tag-id :group-id]))
        threads (reaction (@state :threads))
        open-threads (-> @threads
                         (select-keys @open-thread-ids)
                         vals
                         #_(->> (filter (fn [thread]
                                (or (empty? (thread :tag-ids))
                                    (contains?
                                      (into #{} (map group-for-tag (thread :tag-ids)))
                                      @current-group-id))))))]
      (reaction open-threads)))
