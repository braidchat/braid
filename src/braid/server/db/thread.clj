(ns braid.server.db.thread
  (:require [datomic.api :as d]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-date-time to-long]]
            [braid.server.db.common :refer :all]
            [braid.server.db.tag :as tag]))

(defn thread-group-id
  [conn thread-id]
  (some-> (d/pull (d/db conn) [{:thread/group [:group/id]}]
                  [:thread/id thread-id])
          :thread/group :group/id))

(defn update-thread-last-open!
  [conn thread-id user-id]
  (when (seq (d/q '[:find ?t
                    :in $ ?user-id ?thread-id
                    :where
                    [?u :user/id ?user-id]
                    [?t :thread/id ?thread-id]
                    [?u :user/open-thread ?t]]
                  (d/db conn) user-id thread-id))
    ; TODO: should find a better way of handling this...
    @(d/transact conn
       [[:db/retract [:user/id user-id] :user/open-thread [:thread/id thread-id]]])
    @(d/transact conn
       [[:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]])))

(defn thread-by-id
  [conn thread-id]
  (some-> (d/pull (d/db conn) thread-pull-pattern [:thread/id thread-id])
          db->thread))

(defn threads-by-id
  [conn thread-ids]
  (->> thread-ids
       (map (fn [id] [:thread/id id]))
       (d/pull-many (d/db conn) thread-pull-pattern)
       (map db->thread)))

(defn user-hide-thread!
  [conn user-id thread-id]
  @(d/transact
     conn
     [[:db/retract [:user/id user-id] :user/open-thread [:thread/id thread-id]]]))

(defn user-show-thread!
  [conn user-id thread-id]
  @(d/transact
     conn
     [[:db/add [:user/id user-id] :user/open-thread [:thread/id thread-id]]]))

(defn thread-last-open-at [conn thread user-id]
  (let [user-hides-at (->> (d/q
                             '[:find [?inst ...]
                               :in $ ?thread-id ?user-id
                               :where
                               [?u :user/id ?user-id]
                               [?t :thread/id ?thread-id]
                               [?u :user/open-thread ?t ?tx false]
                               [?tx :db/txInstant ?inst]]
                             (d/history (d/db conn))
                             (thread :id)
                             user-id)
                           (map (fn [t] (.getTime t))))
        user-messages-at (->> (thread :messages)
                              (filter (fn [m] (= (m :user-id) user-id)))
                              (map :created-at)
                              (map (fn [t] (.getTime t))))]
    (apply max (concat [0] user-hides-at user-messages-at))))

(defn thread-add-last-open-at [conn thread user-id]
  (assoc thread :last-open-at (thread-last-open-at conn thread user-id)))

(defn users-subscribed-to-thread
  [conn thread-id]
  (d/q '[:find [?user-id ...]
         :in $ ?thread-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-thread ?thread]
         [?thread :thread/id ?thread-id]]
       (d/db conn)
       thread-id))

(defn user-can-see-thread?
  [conn user-id thread-id]
  (or
    ;user can see the thread if it's a new (i.e. not yet in the database) thread...
    (nil? (d/entity (d/db conn) [:thread/id thread-id]))
    ; ...or they're already subscribed to the thread...
    (contains? (set (users-subscribed-to-thread conn thread-id)) user-id)
    ; ...or they're mentioned in the thread
    ; TODO: is it possible for them to be mentioned but not subscribed?
    (contains? (-> (d/pull (d/db conn) [:thread/mentioned] [:thread/id thread-id])
                   :thread/mentioned set)
               user-id)
    ; ...or they are in the group of any tags on the thread
    (seq (d/q '[:find (pull ?group [:group/id])
                :in $ ?thread-id ?user-id
                :where
                [?thread :thread/id ?thread-id]
                [?thread :thread/tag ?tag]
                [?tag :tag/group ?group]
                [?group :group/user ?user]
                [?user :user/id ?user-id]]
              (d/db conn) thread-id user-id))))

(defn threads-with-tag
  "Find threads with a given tag that the user is allowed to see, ordered by
  most recent message.
  Paginates results, dropping `skip` threads and returning `limit`.
  Returns the threads and a count of how many threads remain."
  [conn user-id tag-id skip limit]
  (let [all-thread-eids (d/q '[:find ?thread (max ?time)
                               :in $ ?tag-id
                               :where
                               [?tag :tag/id ?tag-id]
                               [?thread :thread/tag ?tag]
                               [?msg :message/thread ?thread]
                               [?msg :message/created-at ?time]]
                             (d/db conn) tag-id)
        thread-eids (->> all-thread-eids
                         (sort-by second #(compare %2 %1))
                         (map first)
                         (drop skip)
                         (take limit))]
    {:threads (into ()
                    (comp (map db->thread)
                          (filter #(user-can-see-thread? conn user-id (% :id)))
                          (map #(thread-add-last-open-at conn % user-id)))
                    (d/pull-many (d/db conn) thread-pull-pattern thread-eids))
     :remaining (- (count all-thread-eids) (+ skip (count thread-eids)))}))

(defn open-threads-for-user
  [conn user-id]
  (let [visible-tags (tag/tag-ids-for-user conn user-id)]
    (->> (d/q '[:find (pull ?thread pull-pattern)
                :in $ ?user-id pull-pattern
                :where
                [?e :user/id ?user-id]
                [?e :user/open-thread ?thread]]
              (d/db conn)
              user-id
              thread-pull-pattern)
         (into ()
               (map (comp
                      #(update-in % [:tag-ids]
                                  (partial into #{} (filter visible-tags)))
                      #(thread-add-last-open-at conn % user-id)
                      db->thread
                      first))))))

(defn recent-threads
  [conn {:keys [user-id group-id num-threads] :or {num-threads 10}}]
  (->> (d/q '[:find (pull ?thread pull-pattern)
              :in $ ?group-id ?cutoff pull-pattern
              :where
              [?g :group/id ?group-id]
              [?thread :thread/group ?g]
              [?msg :message/thread ?thread]
              [?msg :message/created-at ?time]
              [(clj-time.coerce/to-date-time ?time) ?dtime]
              [(clj-time.core/after? ?dtime ?cutoff)]]
            (d/db conn)
            group-id
            (t/minus (t/now) (t/weeks 1))
            thread-pull-pattern)
       (into ()
             (comp (map (comp db->thread first))
                   (filter (comp (partial user-can-see-thread? conn user-id) :id))
                   (map #(thread-add-last-open-at conn % user-id))))
       (sort-by (fn [t] (apply max (map (comp to-long :created-at) (t :messages))))
                #(compare %2 %1))
       (take num-threads)))

(defn subscribed-thread-ids-for-user
  [conn user-id]
  (d/q '[:find [?thread-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-thread ?thread]
         [?thread :thread/id ?thread-id]]
       (d/db conn)
       user-id))

(defn user-unsubscribe-from-thread!
  [conn user-id thread-id]
  @(d/transact conn [[:db/retract [:user/id user-id]
                      :user/subscribed-thread [:thread/id thread-id]]
                     [:db/retract [:user/id user-id]
                       :user/open-thread [:thread/id thread-id]]]))
