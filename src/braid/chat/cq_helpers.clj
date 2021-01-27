(ns braid.chat.cq-helpers
  (:require
    [datomic.api :as d]))

(defn user-exists? [db user-id]
  (->> (d/q '[:find ?user .
              :in $ ?user-id
              :where
              [?user :user/id ?user-id]]
            db
            user-id)
       boolean))

(defn group-exists? [db group-id]
  (->> (d/q '[:find ?group .
              :in $ ?group-id
              :where
              [?group :group/id ?user-id]]
            db
            group-id)
       boolean))

(defn thread-exists? [db thread-id]
  (->> (d/q '[:find ?thread .
              :in $ ?thread-id
              :where
              [?thread :thread/id ?thread-id]]
            db
            thread-id)
       boolean))

(defn user-in-group? [db user-id group-id]
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
