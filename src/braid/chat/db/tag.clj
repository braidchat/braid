(ns braid.chat.db.tag
  (:require
    [datomic.api :as d]
    [braid.core.server.db :as db]
    [braid.chat.db.common :refer [create-entity-txn db->tag]]))

;; Queries

(defn tag-group-id
  [tag-id]
  (-> (d/pull (db/db) [{:tag/group [:group/id]}] [:tag/id tag-id])
      (get-in [:tag/group :group/id])))

(defn users-subscribed-to-tag
  [tag-id]
  (d/q '[:find [?user-id ...]
         :in $ ?tag-id
         :where
         [?tag :tag/id ?tag-id]
         [?user :user/subscribed-tag ?tag]
         [?user :user/id ?user-id]]
       (db/db)
       tag-id))

(defn tag-ids-for-user
  "Get all tag ids that are accessible to the user (i.e. are from groups that
  the user is a member of"
  [user-id]
  (->> (d/q '[:find ?tag-id
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?t :tag/group ?g]
              [?t :tag/id ?tag-id]]
            (db/db) user-id)
       (map first)
       set))

(defn tag-statistics-for-user
  [user-id]
  (->> (d/q '[:find
              ?tag-id
              (count-distinct ?th)
              (count-distinct ?sub)
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?t :tag/group ?g]
              [?t :tag/id ?tag-id]
              [?sub :user/subscribed-tag ?t]
              [?th :thread/tag ?t]]
            (db/db) user-id)
       (map (fn [[tag-id threads-count subscribers-count]]
              [tag-id {:tag/threads-count threads-count
                       :tag/subscribers-count subscribers-count}]))
       (into {})))

(defn tags-for-user
  "Get all tags visible to the given user"
  [user-id]
  (let [tag-stats (tag-statistics-for-user user-id)]
    (->> (d/q '[:find
                (pull ?t [:tag/id
                          :tag/name
                          :tag/description
                          {:tag/group [:group/id]}])
                :in $ ?user-id
                :where
                [?u :user/id ?user-id]
                [?g :group/user ?u]
                [?t :tag/group ?g]]
              (db/db) user-id)
         (map (fn [[tag]] (db->tag (merge tag (tag-stats (tag :tag/id))))))
         (into #{}))))

(defn user-in-tag-group?
  [user-id tag-id]
  (seq (d/q '[:find ?g
              :in $ ?user-id ?tag-id
              :where
              [?u :user/id ?user-id]
              [?t :tag/id ?tag-id]
              [?t :tag/group ?g]
              [?g :group/user ?u]]
            (db/db)
            user-id tag-id)))

(defn subscribed-tag-ids-for-user
  [user-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-tag ?tag]
         [?tag :tag/id ?tag-id]]
       (db/db)
       user-id))

;; Transactions

(defn create-tag-txn
  [{:keys [id name group-id]}]
  (create-entity-txn
    {:tag/id id
     :tag/name name
     :tag/group [:group/id group-id]}
    db->tag))

(defn retract-tag-txn
  [tag-id]
  [[:db.fn/retractEntity [:tag/id tag-id]]])

(defn tag-set-description-txn
  [tag-id description]
  [[:db/add [:tag/id tag-id] :tag/description description]])

(defn user-subscribe-to-tag-txn
  [user-id tag-id]
  ; TODO: throw an exception/some sort of error condition if user tried to
  ; subscribe to a tag they can't?
  (if (user-in-tag-group? user-id tag-id)
    [[:db/add [:user/id user-id] :user/subscribed-tag [:tag/id tag-id]]]
    []))

(defn user-unsubscribe-from-tag-txn
  [user-id tag-id]
  [[:db/retract [:user/id user-id] :user/subscribed-tag [:tag/id tag-id]]])
