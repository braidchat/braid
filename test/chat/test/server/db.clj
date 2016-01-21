(ns chat.test.server.db
  (:require [clojure.test :refer :all]
            [chat.test.server.test-utils :refer [fetch-messages]]
            [chat.server.db :as db]
            [chat.server.search :as search]))

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://chat-test"]
                  (db/init!)
                  (db/with-conn (t))
                  (datomic.api/delete-database db/*uri*))))


(deftest create-user
  (let [data {:id (db/uuid)
              :email "foo@bar.com"
              :password "foobar"
              :avatar "http://www.foobar.com/1.jpg"}
        user (db/create-user! data)]
    (testing "can check if an email has been used"
      (is (db/email-taken? (:email data)))
      (is (not (db/email-taken? "baz@quux.net"))))
    (testing "create returns a user"
      (is (= user (-> data (dissoc :password :email) (assoc :nickname "foo")))))
    (testing "can set nickname"
      (is (not (db/nickname-taken? "ol' fooy")))
      @(db/set-nickname! (user :id) "ol' fooy")
      (is (db/nickname-taken? "ol' fooy"))
      (is (= (db/user-with-email "foo@bar.com")
             (-> data (dissoc :password :email) (assoc :nickname "ol' fooy"))))
      (is (= "ol' fooy" (db/get-nickname (user :id)))))

    (testing "user email must be unique"
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/create-user! {:id (db/uuid)
                                     :email (data :email)
                                     :password "zzz"
                                     :avatar "http://zzz.com/2.jpg"}))))
    (testing "user nickname must be unique"
      (let [other {:id (db/uuid)
                   :email "baz@quux.com"
                   :password "foobar"
                   :avatar "foo@bar.com"}]
        (is (some? (db/create-user! other)))
        (is (thrown? java.util.concurrent.ExecutionException @(db/set-nickname! (other :id)  "ol' fooy")))))))

(deftest fetch-users
  (let [user-1-data {:id (db/uuid)
                     :email "foo@bar.com"
                     :password "foobar"
                     :avatar "http://www.foobar.com/1.jpg"}
        user-1 (db/create-user! user-1-data)
        user-2-data  {:id (db/uuid)
                      :email "bar@baz.com"
                      :password "barbaz"
                      :avatar "http://www.barbaz.com/1.jpg"}
        user-2 (db/create-user! user-2-data)
        group (db/create-group! {:id (db/uuid) :name "aoeu"})
        _ (db/user-add-to-group! (user-1 :id) (group :id))
        _ (db/user-add-to-group! (user-2 :id) (group :id))
        users (db/fetch-users-for-user (user-1 :id))]
    (testing "returns all users"
      (is (= (set users) #{user-1 user-2})))
    (testing "get user by email"
      (is (= user-1 (db/user-with-email (user-1-data :email))))
      (is (nil? (db/user-with-email "zzzzz@zzzzzz.ru"))))))

(deftest only-see-users-in-group
  (let [group-1 (db/create-group! {:id (db/uuid) :name "g1"})
        group-2 (db/create-group! {:id (db/uuid) :name "g2"})
        user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@baz.com"
                                 :password "barbaz"
                                 :avatar "http://www.barbaz.com/1.jpg"})
        user-3 (db/create-user! {:id (db/uuid)
                                 :email "quux@baz.com"
                                 :password "barbaz"
                                 :avatar "http://www.barbaz.com/2.jpg"})]
    (is (not (db/user-in-group? (user-1 :id) (group-1 :id))))
    (db/user-add-to-group! (user-1 :id) (group-1 :id))
    (is (db/user-in-group? (user-1 :id) (group-1 :id)))
    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-add-to-group! (user-2 :id) (group-2 :id))
    (db/user-add-to-group! (user-3 :id) (group-2 :id))
    (is (not (db/user-in-group? (user-1 :id) (group-2 :id))))
    (is (= #{user-1 user-2} (db/fetch-users-for-user (user-1 :id))))
    (is (= #{user-1 user-2 user-3} (db/fetch-users-for-user (user-2 :id))))
    (is (= #{user-2 user-3} (db/fetch-users-for-user (user-3 :id))))
    (is (db/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (db/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (db/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (db/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (db/user-visible-to-user? (user-3 :id) (user-2 :id)))))

(deftest authenticate-user
  (let [user-1-data {:id (db/uuid)
                     :email "foo@bar.com"
                     :password "foobar"
                     :avatar ""}
        _ (db/create-user! user-1-data)]

    (testing "returns user-id when email+password matches"
      (is (= (:id user-1-data) (db/authenticate-user (user-1-data :email) (user-1-data :password)))))

    (testing "returns nil when email+password wrong"
      (is (nil? (db/authenticate-user (user-1-data :email) "zzz"))))))

(deftest create-group
  (let [data {:id (db/uuid)
              :name "Lean Pixel"}
        group (db/create-group! data)]
    (testing "can create a group"
      (is (= group (assoc data :users nil))))
    (testing "can add a user to the group"
      (let [user (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})]
        (is (= #{} (db/get-groups-for-user (user :id))))
        (is (= #{} (db/get-users-in-group (group :id))))
        (db/user-add-to-group! (user :id) (group :id))
        (is (= #{data} (db/get-groups-for-user (user :id))))
        (is (= #{user} (db/get-users-in-group (group :id))))))))

(deftest create-message-new
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        thread-id (db/uuid)]

    (testing "create-message w/ new thread-id"
      (let [message-data {:id (db/uuid)
                          :user-id (user-1 :id)
                          :thread-id thread-id
                          :created-at (java.util.Date.)
                          :content "Hello?"}
            message (db/create-message! message-data)]
        (testing "returns message"
          (is (= message-data message)))
        (testing "user has thread open"
          (is (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) thread-id)))
        (testing "user has thread subscribed"
          (is (contains? (set (db/get-subscribed-thread-ids-for-user (user-1 :id))) thread-id)))))

    (testing "create-message w/ existing thread-id"
      (let [message-2-data {:id (db/uuid)
                            :user-id (user-1 :id)
                            :thread-id thread-id
                            :created-at (java.util.Date.)
                            :content "Goodbye."}
            message-2 (db/create-message! message-2-data)]
        (testing "returns message"
          (is (= message-2-data message-2)))
        (testing "user has thread open"
          (is (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) thread-id)))
        (testing "user has thread subscribed"
          (is (contains? (set (db/get-subscribed-thread-ids-for-user (user-1 :id))) thread-id)))))))


(deftest fetch-messages-test
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        message-1-data {:id (db/uuid)
                        :user-id (user-1 :id)
                        :thread-id (db/uuid)
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-1 (db/create-message! message-1-data)
        message-2-data {:id (db/uuid)
                        :user-id (user-1 :id)
                        :thread-id (message-1 :thread-id)
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-2 (db/create-message! message-2-data)
        messages (fetch-messages)]
    (testing "fetch-messages returns all messages"
      (is (= (set messages) #{message-1 message-2})))))

(deftest user-hide-thread
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        message-1 (db/create-message! {:id (db/uuid)
                                       :user-id (user-1 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})
        message-1-b (db/create-message! {:id (db/uuid)
                                         :user-id (user-1 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})
        message-2 (db/create-message! {:id (db/uuid)
                                       :user-id (user-1 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})]
    (testing "thread 1 is open"
      (is (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) (message-1 :thread-id))))
    (testing "thread 2 is open"
      (is (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) (message-2 :thread-id))))
    (testing "user can hide thread"
      (db/user-hide-thread! (user-1 :id) (message-1 :thread-id))
      (is (not (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) (message-1 :thread-id)))))
    (testing "user can hide thread"
      (db/user-hide-thread! (user-1 :id) (message-2 :thread-id))
      (is (not (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) (message-2 :thread-id)))))))

(deftest new-message-opens-for-other-user
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "quux@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        thread-id (db/uuid)
        message-1 (db/create-message! {:id (db/uuid)
                                       :user-id (user-1 :id)
                                       :thread-id thread-id
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})
        message-2 (db/create-message! {:id (db/uuid)
                                       :user-id (user-2 :id)
                                       :thread-id thread-id
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})]
    (testing "both users have open and are subscribed"
      (is (contains? (set (db/get-open-thread-ids-for-user (user-1 :id))) thread-id))
      (is (contains? (set (db/get-open-thread-ids-for-user (user-2 :id))) thread-id))
      (is (contains? (set (db/get-subscribed-thread-ids-for-user (user-1 :id))) thread-id))
      (is (contains? (set (db/get-subscribed-thread-ids-for-user (user-2 :id))) thread-id)))
    (testing "user 2 can hide"
      (db/user-hide-thread! (user-2 :id) thread-id)
      (is (not (contains? (set (db/get-open-thread-ids-for-user (user-2 :id))) thread-id))))
    (testing "if new message by other user, user who hid thread has thread opened"
      (db/create-message! {:id (db/uuid)
                           :user-id (user-1 :id)
                           :thread-id thread-id
                           :created-at (java.util.Date.)
                           :content "Hello?"})
      (is (contains? (set (db/get-open-thread-ids-for-user (user-2 :id))) thread-id)))))

(deftest tags
  (testing "can create tag"
    (let [group (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
          tag-data {:id (db/uuid)
                    :name "acme"
                    :group-id (group :id)}]
      (testing "create-tag!"
        (let [tag (db/create-tag! tag-data)]
          (testing "returns tag"
            (is (= tag (assoc tag-data :group-name "Lean Pixel")))))))))

(deftest user-can-subscribe-to-tags
  (let [user (db/create-user! {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""})
        group (db/create-group! {:id (db/uuid)
                                 :name "Lean Pixel"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group :id)})]
    (db/user-add-to-group! (user :id) (group :id))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/user-subscribe-to-tag! (user :id) (tag-1 :id))
        (db/user-subscribe-to-tag! (user :id) (tag-2 :id)))
      (testing "get-user-subscribed-tags"
        (let [tags (db/get-user-subscribed-tag-ids (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id) (tag-2 :id)}))))))
    (testing "user can unsubscribe from tags"
      (testing "user-unsubscribe-from-tag!"
        (db/user-unsubscribe-from-tag! (user :id) (tag-1 :id))
        (db/user-unsubscribe-from-tag! (user :id) (tag-2 :id)))
      (testing "is unsubscribed"
        (let [tags (db/get-user-subscribed-tag-ids (user :id))]
          (is (= (set tags) #{})))))))

(deftest user-can-only-subscribe-to-tags-in-group
  (let [user (db/create-user! {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""})
        group-1 (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
        group-2 (db/create-group! {:id (db/uuid)
                                   :name "Penyo Pal"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})]
    (db/user-add-to-group! (user :id) (group-1 :id))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/user-subscribe-to-tag! (user :id) (tag-1 :id))
        (db/user-subscribe-to-tag! (user :id) (tag-2 :id)))
      (testing "get-user-subscribed-tags"
        (let [tags (db/get-user-subscribed-tag-ids (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id)}))))))))

(deftest user-can-only-see-tags-in-group
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "quux@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-3 (db/create-user! {:id (db/uuid)
                                 :email "qaax@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        group-1 (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
        group-2 (db/create-group! {:id (db/uuid)
                                   :name "Penyo Pal"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})
        tag-3 (db/create-tag! {:id (db/uuid) :name "acme3" :group-id (group-2 :id)})]
    (db/user-add-to-group! (user-1 :id) (group-1 :id))
    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-add-to-group! (user-2 :id) (group-2 :id))
    (db/user-add-to-group! (user-3 :id) (group-2 :id))
    (testing "user can only see tags in their group(s)"
      (is (= #{tag-1} (db/fetch-tags-for-user (user-1 :id))))
      (is (= #{tag-1 tag-2 tag-3} (db/fetch-tags-for-user (user-2 :id))))
      (is (= #{tag-2 tag-3} (db/fetch-tags-for-user (user-3 :id)))))))

; TODO can add tag to thread (via message)

; TODO can create thread and tag immediately (via message)

(deftest tag-thread-user-subscribe
  (testing "given a user subscribed to a tag"
    (let [group (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
          tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group :id)})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})]
      (db/user-add-to-group! (user-1 :id) (group :id))
      (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))

      (testing "when a thread is tagged with that tag"
        (let [user-2 (db/create-user! {:id (db/uuid)
                                       :email "bar@baz.com"
                                       :password "foobar"
                                       :avatar ""})
              msg (db/create-message! {:id (db/uuid)
                                       :user-id (user-2 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})]
          (db/user-add-to-group! (user-2 :id) (group :id))
          (db/thread-add-tag! (msg :thread-id) (tag-1 :id))
          (testing "then the user is subscribed to that thread"
            (let [user-threads (db/get-subscribed-thread-ids-for-user (user-1 :id))]
              (is (contains? (set user-threads) (msg :thread-id))))
            (let [users (db/get-users-subscribed-to-thread (msg :thread-id))]
              (is (contains? (set users) (user-1 :id)))))
          (testing "then the user has that thread open"
            (let [user-threads (db/get-open-thread-ids-for-user (user-1 :id))]
              (is (contains? (set user-threads) (msg :thread-id))))))))))

(deftest user-thread-visibility
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "quux@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-3 (db/create-user! {:id (db/uuid)
                                 :email "qaax@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        group-1 (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
        group-2 (db/create-group! {:id (db/uuid)
                                   :name "Penyo Pal"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})

        thread-1-id (db/uuid)
        thread-2-id (db/uuid)]

    (testing "everyone can see threads because they haven't been created"
      (is (db/user-can-see-thread? (user-1 :id) thread-1-id))
      (is (db/user-can-see-thread? (user-2 :id) thread-1-id))
      (is (db/user-can-see-thread? (user-3 :id) thread-1-id)))

    (db/create-message! {:thread-id thread-1-id :id (db/uuid) :content "zzz"
                         :user-id (user-1 :id) :created-at (java.util.Date.)})
    (db/create-message! {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                         :user-id (user-2 :id) :created-at (java.util.Date.)})

    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))
    (db/thread-add-tag! thread-1-id (tag-1 :id))
    (db/thread-add-tag! thread-2-id (tag-2 :id))

    (db/user-add-to-group! (user-3 :id) (group-2 :id))

    (testing "user 1 can see thread 1 because they created it"
      (is (db/user-can-see-thread? (user-1 :id) thread-1-id)))
    (testing "user 1 can't see thread 2"
      (is (not (db/user-can-see-thread? (user-1 :id) thread-2-id))))

    (testing "user 2 can see thread 1 because they've already been subscribed"
      (is (db/user-can-see-thread? (user-2 :id) thread-1-id)))
    (testing "user 2 can see thread 2 because they created it"
      (is (db/user-can-see-thread? (user-2 :id) thread-2-id)))

    (testing "user 3 can't see thread 1"
      (is (not (db/user-can-see-thread? (user-3 :id) thread-1-id))))
    (testing "user 3 can see thread 2 because they have the tag"
      (is (db/user-can-see-thread? (user-3 :id) thread-2-id)))))

(deftest user-invite-to-group
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@baz.com"
                                 :password "foobar"
                                 :avatar ""})
        group (db/create-group! {:name "group 1" :id (db/uuid)})]
    (db/user-add-to-group! (user-1 :id) (group :id))
    (is (empty? (db/fetch-invitations-for-user (user-1 :id))))
    (is (empty? (db/fetch-invitations-for-user (user-2 :id))))
    (let [invite-id (db/uuid)
          invite (db/create-invitation! {:id invite-id
                                         :inviter-id (user-1 :id)
                                         :invitee-email "bar@baz.com"
                                         :group-id (group :id)})]
      (is (= invite (db/get-invite invite-id)))
      (is (seq (db/fetch-invitations-for-user (user-2 :id))))
      (db/retract-invitation! invite-id)
      (is (empty? (db/fetch-invitations-for-user (user-2 :id)))))))

(deftest adding-user-to-group-subscribes-tags
  (let [user (db/create-user! {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""})
        group (db/create-group! {:name "group" :id (db/uuid)})
        group-tags (doall
                     (map db/create-tag!
                          [{:id (db/uuid) :name "t1" :group-id (group :id)}
                           {:id (db/uuid) :name "t2" :group-id (group :id)}
                           {:id (db/uuid) :name "t3" :group-id (group :id)}]))]
    (testing "some misc functions"
      (is (= group (db/get-group (group :id))))
      (is (= (set group-tags)
             (set (db/get-group-tags (group :id))))))
    (db/user-add-to-group! (user :id) (group :id))
    (db/user-subscribe-to-group-tags! (user :id) (group :id))
    (is (= (set (db/get-user-subscribed-tag-ids (user :id)))
           (db/get-user-visible-tag-ids (user :id))
           (set (map :id group-tags))))))

(deftest tagging-thread-with-disjoint-groups
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@foo.com"
                                 :password "foobar"
                                 :avatar ""})
        group-1 (db/create-group! {:name "group1" :id (db/uuid)})
        group-2 (db/create-group! {:name "group2" :id (db/uuid)})
        tag-1 (db/create-tag! {:id (db/uuid) :name "tag1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "tag2" :group-id (group-2 :id)})
        thread-id (db/uuid)]

    (db/user-add-to-group! (user-1 :id) (group-1 :id))
    (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))

    (db/user-add-to-group! (user-1 :id) (group-2 :id))
    (db/user-subscribe-to-tag! (user-1 :id) (tag-2 :id))

    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))

    (db/create-message! {:thread-id thread-id :id (db/uuid) :content "zzz"
                         :user-id (user-1 :id) :created-at (java.util.Date.)})
    (db/thread-add-tag! thread-id (tag-1 :id))
    (db/thread-add-tag! thread-id (tag-2 :id))

    (is (db/user-can-see-thread? (user-1 :id) thread-id))
    (is (db/user-can-see-thread? (user-2 :id) thread-id))
    (let [u1-threads (db/get-open-threads-for-user (user-1 :id))
          u2-threads (db/get-open-threads-for-user (user-2 :id))]
      (is (= 1 (count u1-threads)))
      (is (= 1 (count u2-threads)))
      (is (= #{(tag-1 :id) (tag-2 :id)}
             (set (:tag-ids (first u1-threads)))))
      (is (= #{(tag-1 :id)}
             (set (:tag-ids (first u2-threads))))))))

(deftest mentioning-users-in-threads
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@foo.com"
                                 :password "foobar"
                                 :avatar ""}) ]

    (db/set-nickname! (user-1 :id) "foo")
    (db/set-nickname! (user-2 :id) "bar")

    (testing "Can mention a user to open the thread for them"
      (let [thread-id (db/uuid)]
        (db/create-message! {:thread-id thread-id :id (db/uuid) :content  "hi @bar"
                             :user-id (user-1 :id) :created-at (java.util.Date.)})
        (is (not (db/user-can-see-thread? (user-2 :id) thread-id)))
        (is (empty? (db/get-open-thread-ids-for-user (user-2 :id))))
        (db/thread-add-mentioned! thread-id (user-2 :id))
        (is (db/user-can-see-thread? (user-2 :id) thread-id))
        (is (= [thread-id] (db/get-open-thread-ids-for-user (user-2 :id))))

        (testing "and the thread has a collection of mentions"
          (is  (= [(user-2 :id)]
                  (-> (db/get-thread thread-id) :mentioned-ids)
                  (-> (db/get-open-threads-for-user (user-2 :id))
                      first
                      :mentioned-ids))))))))
