(ns chat.server.seed
  (:require [chat.server.db :as db]
            [environ.core :refer [env]]))

(defn drop! []
  (datomic.api/delete-database db/*uri*))

(defn init! []
  (db/init!))

(defn seed! []
  (db/with-conn
    (let [group-1 (db/create-group! {:id (db/uuid) :name "Braid"})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@example.com"
                                   :nickname "foo"
                                   :password "foo"
                                   :avatar ""})
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "bar@example.com"
                                   :nickname "bar"
                                   :password "bar"
                                   :avatar ""})
          _ (db/user-add-to-group! (user-1 :id) (group-1 :id))
          _ (db/user-add-to-group! (user-2 :id) (group-1 :id))

          tag-1 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "braid"})
          tag-2 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "watercooler"})

          _ (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))
          _ (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))

          _ (db/user-subscribe-to-tag! (user-1 :id) (tag-2 :id))
          _ (db/user-subscribe-to-tag! (user-2 :id) (tag-2 :id))

          msg (db/create-message! {:id (db/uuid)
                                   :user-id (user-1 :id)
                                   :thread-id (db/uuid)
                                   :created-at (java.util.Date.)
                                   :content "Hello?"
                                   :mentioned-tag-ids [(tag-1 :id)]})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (user-2 :id)
                                 :created-at (java.util.Date.)
                                 :content "Hi!"})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (user-1 :id)
                                 :created-at (java.util.Date.)
                                 :content "Oh, great, someone else is here."})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (user-2 :id)
                                 :created-at (java.util.Date.)
                                 :content "Yep"})]
      (println "users" user-1 user-2)
      (println "tags" tag-2 tag-2))))
