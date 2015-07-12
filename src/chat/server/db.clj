(ns chat.server.db)

(def DB
  {:users
   [{:id 1234
     :email "raf@leanpixel.com"
     :password "foobar"
     :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-08/4801271456_41230ac0b881cdf85c28_72.jpg"}
    {:id 5678
     :email "james@leanpixel.com"
     :password "foobar"
     :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-09/4805955000_07dc272f0a7f9de7719e_192.jpg"}]
   :messages
   [{:id 99 :thread-id 123 :user-id 1234 :created-at (java.util.Date.)
     :content "Hello?"}
    {:id 98 :thread-id 123 :user-id 5678 :created-at (java.util.Date.)
     :content "Hi!"}
    {:id 97 :thread-id 123 :user-id 1234 :created-at (java.util.Date.)
     :content "Oh, great, someone else is here."}
    {:id 96 :thread-id 123 :user-id 5678 :created-at (java.util.Date.)
     :content "Yep"}]})

(defn authenticate
  "returns user-id if email and password are correct"
  [email password]
  (->> (DB :users)
       (filter (fn [u] (and
                         (= (u :email) email)
                         (= (u :password) password))))
       first
       :id))

(defn all-users []
  (DB :users))

(defn all-messages []
  (DB :messages))
