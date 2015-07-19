(ns chat.server.seed
  (:require [chat.server.db :as db]))

(defn seed! []
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "raf@leanpixel.com"
                                 :password "foobar"
                                 :avatar "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-08/4801271456_41230ac0b881cdf85c28_72.jpg"})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "james@leanpixel.com"
                                 :password "foobar"
                                 :avatar "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-09/4805955000_07dc272f0a7f9de7719e_192.jpg"})

        msg (db/create-message! {:id (db/uuid)
                                 :user-id (user-1 :id)
                                 :thread-id (db/uuid)
                                 :created-at (java.util.Date.)
                                 :content "Hello?"})
        _ (db/create-message! {:id (db/uuid)
                               :thread-id (msg :thread-id)
                               :user-id (user-1 :id)
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
                               :content "Yep"})]))
