(ns braid.server.seed
  (:require [braid.server.db :as db]
            [braid.server.db.group :as group]
            [braid.server.db.message :as message]
            [braid.server.db.tag :as tag]
            [braid.server.db.user :as user]
            [braid.server.conf :refer [config]]))

(defn drop! []
  (datomic.api/delete-database (config :db-url)))

(defn init! []
  (db/init! (config :db-url)))

(defn seed! []
  (let [group-1 (group/create-group! db/conn {:id (db/uuid) :name "Braid"})
        group-2 (group/create-group! db/conn {:id (db/uuid) :name "Chat"})
        user-1 (user/create-user! db/conn
                                  {:id (db/uuid)
                                   :email "foo@example.com"
                                   :nickname "foo"
                                   :password "foo"
                                   :avatar "data:image/gif;base64,R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="})
        user-2 (user/create-user! db/conn
                                  {:id (db/uuid)
                                   :email "bar@example.com"
                                   :nickname "bar"
                                   :password "bar"
                                   :avatar "data:image/gif;base64,R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="})
        _ (group/user-add-to-group! db/conn (user-1 :id) (group-1 :id))
        _ (group/user-add-to-group! db/conn (user-2 :id) (group-1 :id))
        _ (group/user-add-to-group! db/conn (user-1 :id) (group-2 :id))
        _ (group/user-add-to-group! db/conn (user-2 :id) (group-2 :id))

        _ (group/user-make-group-admin! db/conn (user-1 :id) (group-1 :id))
        _ (group/user-make-group-admin! db/conn (user-2 :id) (group-2 :id))

        tag-1 (tag/create-tag! db/conn {:id (db/uuid) :group-id (group-1 :id) :name "braid"})
        tag-2 (tag/create-tag! db/conn {:id (db/uuid) :group-id (group-1 :id) :name "watercooler"})

        _ (tag/user-subscribe-to-tag! db/conn (user-1 :id) (tag-1 :id))
        _ (tag/user-subscribe-to-tag! db/conn (user-2 :id) (tag-1 :id))

        _ (tag/user-subscribe-to-tag! db/conn (user-1 :id) (tag-2 :id))
        _ (tag/user-subscribe-to-tag! db/conn (user-2 :id) (tag-2 :id))

        msg (message/create-message! db/conn {:id (db/uuid)
                                              :group-id (group-1 :id)
                                              :user-id (user-1 :id)
                                              :thread-id (db/uuid)
                                              :created-at (java.util.Date.)
                                              :content "Hello?"
                                              :mentioned-tag-ids [(tag-1 :id)]})
        _ (message/create-message! db/conn {:id (db/uuid)
                                            :group-id (group-1 :id)
                                            :thread-id (msg :thread-id)
                                            :user-id (user-2 :id)
                                            :created-at (java.util.Date.)
                                            :content "Hi!"})
        _ (message/create-message! db/conn {:id (db/uuid)
                                            :thread-id (msg :thread-id)
                                            :group-id (group-1 :id)
                                            :user-id (user-1 :id)
                                            :created-at (java.util.Date.)
                                            :content "Oh, great, someone else is here."})
        _ (message/create-message! db/conn {:id (db/uuid)
                                            :group-id (group-1 :id)
                                            :thread-id (msg :thread-id)
                                            :user-id (user-2 :id)
                                            :created-at (java.util.Date.)
                                            :content "Yep"})

        tag-3 (tag/create-tag! db/conn {:id (db/uuid) :group-id (group-2 :id) :name "abcde"})

        _ (tag/user-subscribe-to-tag! db/conn (user-1 :id) (tag-3 :id))
        _ (tag/user-subscribe-to-tag! db/conn (user-2 :id) (tag-3 :id))

        msg (message/create-message! db/conn {:id (db/uuid)
                                              :user-id (user-1 :id)
                                              :group-id (group-2 :id)
                                              :thread-id (db/uuid)
                                              :created-at (java.util.Date.)
                                              :content "Hello?"
                                              :mentioned-tag-ids [(tag-3 :id)]})
        _ (message/create-message! db/conn {:id (db/uuid)
                                            :group-id (group-2 :id)
                                            :thread-id (msg :thread-id)
                                            :user-id (user-2 :id)
                                            :created-at (java.util.Date.)
                                            :content "Hi!"})]
    (println "users" user-1 user-2)
    (println "tags" tag-2 tag-2)))
