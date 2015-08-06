(ns chat.server.seed
  (:require [chat.server.db :as db]
            [environ.core :refer [env]]))

(defn drop! []
  (datomic.api/delete-database db/*uri*))

(defn init! []
  (db/init!))

(defn seed! []
  (db/with-conn
    (let [group-1 (db/create-group! {:id (db/uuid)
                                     :name "Lean Pixel"})
          group-2 (db/create-group! {:id (db/uuid)
                                     :name "Penyo Pal"})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "raf@leanpixel.com"
                                   :password (get env :rafal-password "foobar")
                                   :avatar "https://s3.amazonaws.com/chat.leanpixel.com/avatars/4801271456_41230ac0b881cdf85c28_72.jpg"})
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "james@leanpixel.com"
                                   :password (get env :james-password "foobar")
                                   :avatar "https://s3.amazonaws.com/chat.leanpixel.com/avatars/4805955000_07dc272f0a7f9de7719e_192.jpg"})
          _ (db/user-add-to-group! (user-1 :id) (group-1 :id))
          _ (db/user-add-to-group! (user-1 :id) (group-2 :id))
          _ (db/user-add-to-group! (user-2 :id) (group-1 :id))
          _ (db/user-add-to-group! (user-2 :id) (group-2 :id))
          user-3 (db/create-user! {:id (db/uuid)
                                   :email "test@ycombinator.com"
                                   :password (get env :tester-password "")
                                   :avatar "https://s3.amazonaws.com/chat.leanpixel.com/ycombinator-logo.png"})

          tag-1 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "idonethis"})
          tag-2 (db/create-tag! {:id (db/uuid) :group-id (group-1 :id) :name "watercooler"})

          tag-3 (db/create-tag! {:id (db/uuid) :group-id (group-2 :id) :name "idonethis"})
          tag-4 (db/create-tag! {:id (db/uuid) :group-id (group-2 :id) :name "watercooler"})

          _ (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))
          _ (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))
          _ (db/user-subscribe-to-tag! (user-3 :id) (tag-1 :id))

          _ (db/user-subscribe-to-tag! (user-1 :id) (tag-2 :id))
          _ (db/user-subscribe-to-tag! (user-2 :id) (tag-2 :id))
          _ (db/user-subscribe-to-tag! (user-3 :id) (tag-2 :id))

          msg (db/create-message! {:id (db/uuid)
                                   :user-id (user-1 :id)
                                   :thread-id (db/uuid)
                                   :created-at (java.util.Date.)
                                   :content "Hello?"})
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
                                 :content "Yep"})
          _ (db/thread-add-tag! (msg :thread-id) (tag-1 :id))]
      (println "users" user-1 user-2)
      (println "tags" tag-2 tag-2))))
