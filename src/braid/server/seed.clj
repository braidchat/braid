(ns braid.server.seed
  (:require [braid.server.db :as db]
            [braid.server.conf :refer [config]]))

(defn drop! []
  (datomic.api/delete-database (config :db-url)))

(defn init! []
  (db/init! (config :db-url)))

(defn seed! []
  (let [group-1 (db/create-group! {:id (db/uuid) :name "Braid"})
        group-2 (db/create-group! {:id (db/uuid) :name "Chat"})
        user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@example.com"
                                 :nickname "foo"
                                 :password "foo"
                                 :avatar "data:image/gif;base64,R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@example.com"
                                 :nickname "bar"
                                 :password "bar"
                                 :avatar "data:image/gif;base64,R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="})
        _ (db/user-add-to-group! (user-1 :id) (group-1 :id))
        _ (db/user-add-to-group! (user-2 :id) (group-1 :id))
        _ (db/user-add-to-group! (user-1 :id) (group-2 :id))
        _ (db/user-add-to-group! (user-2 :id) (group-2 :id))

        tag-1 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "braid"})
        tag-2 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "watercooler"})

        _ (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))
        _ (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))

        _ (db/user-subscribe-to-tag! (user-1 :id) (tag-2 :id))
        _ (db/user-subscribe-to-tag! (user-2 :id) (tag-2 :id))

        msg (db/create-message! {:id (db/uuid)
                                 :group-id (group-1 :id)
                                 :user-id (user-1 :id)
                                 :thread-id (db/uuid)
                                 :created-at (java.util.Date.)
                                 :content "Hello?"
                                 :mentioned-tag-ids [(tag-1 :id)]})
        _ (db/create-message! {:id (db/uuid)
                               :group-id (group-1 :id)
                               :thread-id (msg :thread-id)
                               :user-id (user-2 :id)
                               :created-at (java.util.Date.)
                               :content "Hi!"})
        _ (db/create-message! {:id (db/uuid)
                               :thread-id (msg :thread-id)
                               :group-id (group-1 :id)
                               :user-id (user-1 :id)
                               :created-at (java.util.Date.)
                               :content "Oh, great, someone else is here."})
        _ (db/create-message! {:id (db/uuid)
                               :group-id (group-1 :id)
                               :thread-id (msg :thread-id)
                               :user-id (user-2 :id)
                               :created-at (java.util.Date.)
                               :content "Yep"})

        tag-3 (db/create-tag! {:id (db/uuid) :group-id (group-2 :id) :name "abcde"})

        _ (db/user-subscribe-to-tag! (user-1 :id) (tag-3 :id))
        _ (db/user-subscribe-to-tag! (user-2 :id) (tag-3 :id))

        msg (db/create-message! {:id (db/uuid)
                                 :user-id (user-1 :id)
                                 :group-id (group-2 :id)
                                 :thread-id (db/uuid)
                                 :created-at (java.util.Date.)
                                 :content "Hello?"
                                 :mentioned-tag-ids [(tag-3 :id)]})
        _ (db/create-message! {:id (db/uuid)
                               :group-id (group-2 :id)
                               :thread-id (msg :thread-id)
                               :user-id (user-2 :id)
                               :created-at (java.util.Date.)
                               :content "Hi!"})]
    (println "users" user-1 user-2)
    (println "tags" tag-2 tag-2)))
