(ns braid.chat.cq-helpers
  (:require
    [datomic.api :as d]
    [braid.chat.db.thread :as db.thread]))

(defn user-exists?
  [db user-id]
  (->> (d/q '[:find ?user .
              :in $ ?user-id
              :where
              [?user :user/id ?user-id]]
            db
            user-id)
       boolean))

(defn group-exists?
  [db group-id]
  (->> (d/q '[:find ?group .
              :in $ ?group-id
              :where
              [?group :group/id ?user-id]]
            db
            group-id)
       boolean))

(defn thread-exists?
  [db thread-id]
  (->> (d/q '[:find ?thread .
              :in $ ?thread-id
              :where
              [?thread :thread/id ?thread-id]]
            db
            thread-id)
       boolean))

(defn user-in-group?
  [db user-id group-id]
  (->> (d/q '[:find ?user .
              :in $ ?user-id ?group-id
              :where
              [?group :group/id ?group-id]
              [?user :user/id ?user-id]
              [?group :group/user ?user]]
            db
            user-id
            group-id)
       boolean))

(defn user-has-thread-open?
  [db user-id thread-id]
  (->> (d/q '[:find ?user .
              :in $ ?user-id ?thread-id
              :where
              [?thread :thread/id ?thread-id]
              [?user :user/id ?user-id]
              [?user :user/open-thread ?thread]]
            db
            user-id
            thread-id)
       boolean))

(defn user-can-access-thread?
  [db user-id thread-id]
  (->> (db.thread/user-can-see-thread? user-id thread-id)
       boolean))
