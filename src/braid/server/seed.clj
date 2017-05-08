(ns braid.server.seed
  (:require
    [braid.server.conf :refer [config]]
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.db.message :as message]
    [braid.server.db.tag :as tag]
    [braid.server.db.user :as user]))

(defn drop! []
  (datomic.api/delete-database (config :db-url)))

(defn init! []
  (db/init! (config :db-url)))

(defn seed! []
  (let [_ (println "Create Groups")
        [group-1 group-2]
        (db/run-txns!
          (concat
            (group/create-group-txn {:id (db/uuid) :slug "braid" :name "Braid"})
            (group/create-group-txn {:id (db/uuid) :slug "chat" :name "Chat"})))

        _ (println "Create Users")
        [user-1 user-2]
        (db/run-txns!
          (concat
            (user/create-user-txn
              {:id (db/uuid)
               :email "foo@example.com"})
            (user/create-user-txn
              {:id (db/uuid)
               :email "bar@example.com"})))

        _ (println "Set Passwords")
        _ (db/run-txns! (concat
                          (user/set-user-password-txn user-1 "foofoofoo")
                          (user/set-user-password-txn user-2 "barbarbar")))

        _ (println "Add Users to Groups and Make Users Admins")
        _ (db/run-txns!
            (concat
              (group/user-add-to-group-txn (user-1 :id) (group-1 :id))
              (group/user-add-to-group-txn (user-2 :id) (group-1 :id))
              (group/user-add-to-group-txn (user-1 :id) (group-2 :id))
              (group/user-add-to-group-txn (user-2 :id) (group-2 :id))
              (group/user-make-group-admin-txn (user-1 :id) (group-1 :id))
              (group/user-make-group-admin-txn (user-2 :id) (group-2 :id))))

        _ (println "Create Tags")
        [tag-1 tag-2]
        (db/run-txns!
          (concat
            (tag/create-tag-txn {:id (db/uuid) :group-id (group-1 :id) :name "braid"})
            (tag/create-tag-txn {:id (db/uuid) :group-id (group-1 :id) :name "watercooler"})))

        _ (println "Subscribe Users to Tags")
        _ (db/run-txns!
            (concat
              (tag/user-subscribe-to-tag-txn (user-1 :id) (tag-1 :id))
              (tag/user-subscribe-to-tag-txn (user-2 :id) (tag-1 :id))
              (tag/user-subscribe-to-tag-txn (user-1 :id) (tag-2 :id))
              (tag/user-subscribe-to-tag-txn (user-2 :id) (tag-2 :id))))

        _ (println "Create Messages")
        [msg-1]
        (db/run-txns!
          (message/create-message-txn {:id (db/uuid)
                                       :group-id (group-1 :id)
                                       :user-id (user-1 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"
                                       :mentioned-tag-ids [(tag-1 :id)]}))
        [msg-2 msg-3 msg-4]
        (db/run-txns!
          (concat
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group-1 :id)
                                         :thread-id (msg-1 :thread-id)
                                         :user-id (user-2 :id)
                                         :created-at (java.util.Date.)
                                         :content "Hi!"})
            (message/create-message-txn {:id (db/uuid)
                                         :thread-id (msg-1 :thread-id)
                                         :group-id (group-1 :id)
                                         :user-id (user-1 :id)
                                         :created-at (java.util.Date.)
                                         :content "Oh, great, someone else is here."})
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group-1 :id)
                                         :thread-id (msg-1 :thread-id)
                                         :user-id (user-2 :id)
                                         :created-at (java.util.Date.)
                                         :content "Yep"})))

        [tag-3] (db/run-txns! (tag/create-tag-txn {:id (db/uuid) :group-id (group-2 :id) :name "abcde"}))

        _ (db/run-txns!
            (concat
              (tag/user-subscribe-to-tag-txn (user-1 :id) (tag-3 :id))
              (tag/user-subscribe-to-tag-txn (user-2 :id) (tag-3 :id))))

        [msg5 msg6]
        (db/run-txns!
          (concat
            (message/create-message-txn {:id (db/uuid)
                                         :user-id (user-1 :id)
                                         :group-id (group-2 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"
                                         :mentioned-tag-ids [(tag-3 :id)]})
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group-2 :id)
                                         :thread-id (msg-1 :thread-id)
                                         :user-id (user-2 :id)
                                         :created-at (java.util.Date.)
                                         :content "Hi!"})))]))
