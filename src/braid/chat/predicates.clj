(ns braid.chat.predicates
  (:require
    [datomic.api :as d]
    [braid.chat.db.thread :as db.thread]))

;; *-exists?

(defn ^:private exists?
  [db id-key entity-id]
  (->> (d/q '[:find ?entity .
              :in $ ?entity-id ?key
              :where
              [?entity ?key ?entity-id]]
            db
            entity-id
            id-key)
       boolean))

(defn user-exists?
  [db user-id]
  (exists? db :user/id user-id))

(defn group-exists?
  [db group-id]
  (exists? db :group/id group-id))

(defn thread-exists?
  [db thread-id]
  (exists? db :thread/id thread-id))

(defn tag-exists?
  [db tag-id]
  (exists? db :tag/id tag-id))

(defn message-exists?
  [db message-id]
  (exists? db :message/id message-id))

;; OTHER

(defn tag-in-group-with-name-exists?
  [db group-id tag-name]
  (->> (d/q '[:find ?tag .
              :in $ ?group-id ?tag-name
              :where
              [?group :group/id ?group-id]
              [?tag :tag/name ?tag-name]
              [?tag :tag/group ?group]]
            db
            group-id
            tag-name)
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

(defn thread-user-same-group?
  [db thread-id user-id]
  (->> (d/q '[:find ?user .
              :in $ ?user-id ?thread-id
              :where
              [?user :user/id ?user-id]
              [?thread :thread/id ?thread-id]
              [?thread :thread/group ?group]
              [?group :group/user ?user]]
            db
            user-id
            thread-id)
       boolean))

(defn thread-tag-same-group?
  [db thread-id user-id]
  (->> (d/q '[:find ?tag .
              :in $ ?user-id ?thread-id
              :where
              [?thread :thread/id ?thread-id]
              [?tag :tag/id ?tag-id]
              [?tag :tag/group ?group]
              [?thread :thread/group ?group]]
            db
            user-id
            thread-id)
       boolean))
