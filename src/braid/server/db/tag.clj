(ns braid.server.db.tag
  (:require [datomic.api :as d]
            [braid.server.db.common :refer :all]))

(defn create-tag!
  [conn attrs]
  (->> {:tag/id (attrs :id)
        :tag/name (attrs :name)
        :tag/group [:group/id (attrs :group-id)]}
       (create-entity! conn)
       db->tag))

(defn retract-tag!
  [conn tag-id]
  @(d/transact conn [[:db.fn/retractEntity [:tag/id tag-id]]]))

(defn tag-group-id [conn tag-id]
  (-> (d/pull (d/db conn) [{:tag/group [:group/id]}] [:tag/id tag-id])
      (get-in [:tag/group :group/id])))

(defn tag-set-description!
  [conn tag-id description]
  @(d/transact conn [[:db/add [:tag/id tag-id]
                      :tag/description description]]))

(defn users-subscribed-to-tag
  [conn tag-id]
  (d/q '[:find [?user-id ...]
         :in $ ?tag-id
         :where
         [?tag :tag/id ?tag-id]
         [?user :user/subscribed-tag ?tag]
         [?user :user/id ?user-id]]
       (d/db conn)
       tag-id))

(defn tag-ids-for-user
  "Get all tag ids that are accessible to the user (i.e. are from groups that
  the user is a member of"
  [conn user-id]
  (->> (d/q '[:find ?tag-id
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?t :tag/group ?g]
              [?t :tag/id ?tag-id]]
            (d/db conn) user-id)
       (map first)
       set))

(defn tag-statistics-for-user
  [conn user-id]
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
            (d/db conn) user-id)
       (map (fn [[tag-id threads-count subscribers-count]]
              [tag-id {:tag/threads-count threads-count
                       :tag/subscribers-count subscribers-count}]))
       (into {})))

(defn tags-for-user
  "Get all tags visible to the given user"
  [conn user-id]
  (let [tag-stats (tag-statistics-for-user conn user-id)]
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
              (d/db conn) user-id)
         (map (fn [[tag]]
                (db->tag (merge tag (tag-stats (tag :tag/id))))))
         (into #{}))))

(defn user-in-tag-group? [conn user-id tag-id]
  (seq (d/q '[:find ?g
              :in $ ?user-id ?tag-id
              :where
              [?u :user/id ?user-id]
              [?t :tag/id ?tag-id]
              [?t :tag/group ?g]
              [?g :group/user ?u]]
            (d/db conn)
            user-id tag-id)))

(defn user-subscribe-to-tag! [conn user-id tag-id]
  ; TODO: throw an exception/some sort of error condition if user tried to
  ; subscribe to a tag they can't?
  (when (user-in-tag-group? conn user-id tag-id)
    @(d/transact conn [[:db/add [:user/id user-id]
                        :user/subscribed-tag [:tag/id tag-id]]])))

(defn user-unsubscribe-from-tag!
  [conn user-id tag-id]
  @(d/transact conn [[:db/retract [:user/id user-id]
                      :user/subscribed-tag [:tag/id tag-id]]]))

(defn subscribed-tag-ids-for-user
  [conn user-id]
  (d/q '[:find [?tag-id ...]
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]
         [?user :user/subscribed-tag ?tag]
         [?tag :tag/id ?tag-id]]
       (d/db conn)
       user-id))
