(ns braid.test.server.new-message
  (:require
    [clojure.test :refer :all]
    [braid.core.server.db :as db]
    [braid.core.server.db.group :as group]
    [braid.core.server.db.message :as message]
    [braid.core.server.db.tag :as tag]
    [braid.core.server.db.thread :as thread]
    [braid.core.server.db.user :as user]
    [braid.test.fixtures.db :refer [drop-db]]))

(use-fixtures :each drop-db)

(deftest create-message
  (let [[user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "foo@bar.com"
                                                      :password "foobar"
                                                      :avatar "http://www.foobar.com/1.jpg"}))
        thread-id (db/uuid)]

    (testing "new messages can create a thread"
      (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "group" :name "group"}))
            message-data {:id (db/uuid)
                          :group-id (group :id)
                          :user-id (user-1 :id)
                          :thread-id thread-id
                          :created-at (java.util.Date.)
                          :content "Hello?"}
            [message] (db/run-txns! (message/create-message-txn message-data))]

        (testing "returns message"
          (is (= message-data message)))

        (testing "user has thread open"
          (is (contains?
                (set (map :id (thread/open-threads-for-user (user-1 :id))))
                thread-id)))

        (testing "user has thread subscribed"
          (is (contains?
                (set (thread/subscribed-thread-ids-for-user (user-1 :id)))
                thread-id)))))

    (testing "new message can add to an existing thread"
      (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "group2" :name "group2"}))
            message-2-data {:id (db/uuid)
                            :group-id (group :id)
                            :user-id (user-1 :id)
                            :thread-id thread-id
                            :created-at (java.util.Date.)
                            :content "Goodbye."}
            [message-2] (db/run-txns! (message/create-message-txn message-2-data))]

        (testing "returns message"
          (is (= message-2-data message-2)))

        (testing "user has thread open"
          (is (contains?
                (set (map :id (thread/open-threads-for-user (user-1 :id))))
                thread-id)))

        (testing "user has thread subscribed"
          (is (contains?
                (set (thread/subscribed-thread-ids-for-user (user-1 :id)))
                thread-id)))))))

(deftest new-message-opens-and-subscribes

  (testing "given a user"
    (let [[user-1 group] (db/run-txns!
                           (concat
                             (user/create-user-txn {:id (db/uuid)
                                                    :email "foo@bar.com"
                                                    :password "foobar"
                                                    :avatar ""})
                             (group/create-group-txn {:id (db/uuid) :slug "group" :name "group"})))]

      (testing "when the user sends a new message"
        (let [thread-id (db/uuid)]
          (db/run-txns!
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id thread-id
                                         :created-at (java.util.Date.)
                                         :content "Hello?"}))
          (testing "then the user is subscribed to the thread"
            (is (contains?
                  (set (map :id (thread/open-threads-for-user (user-1 :id))))
                  thread-id)))

          (testing "then the user has the thread open"
            (is (contains?
                  (set (thread/subscribed-thread-ids-for-user (user-1 :id)))
                  thread-id))))))))

(deftest new-message-opens-thread

  (testing "given a thread with 2 participants"
    (let [[user-1 user-2 group] (db/run-txns!
                                  (concat
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "foo@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "quux@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (group/create-group-txn {:id (db/uuid)
                                                             :slug "group"
                                                             :name "group"})))
          thread-id (db/uuid)
          message-1 (db/run-txns!
                      (message/create-message-txn {:id (db/uuid)
                                                   :group-id (group :id)
                                                   :user-id (user-1 :id)
                                                   :thread-id thread-id
                                                   :created-at (java.util.Date.)
                                                   :content "Hello?"}))
          message-2 (db/run-txns!
                      (message/create-message-txn {:id (db/uuid)
                                                   :group-id (group :id)
                                                   :user-id (user-2 :id)
                                                   :thread-id thread-id
                                                   :created-at (java.util.Date.)
                                                   :content "Hello?"}))]

      (testing "when user-2 hides the thread"
        (db/run-txns! (thread/user-hide-thread-txn (user-2 :id) thread-id))

        (testing "then user-2 no longer has the thread open"
          (is (not (contains?
                     (set (map :id (thread/open-threads-for-user (user-2 :id))))
                     thread-id)))))

      (testing "when user-1 sends another message in the thread"
        (db/run-txns! (message/create-message-txn
                        {:id (db/uuid)
                         :group-id (group :id)
                         :user-id (user-1 :id)
                         :thread-id thread-id
                         :created-at (java.util.Date.)
                         :content "Hello?"}))

        (testing "then user-2 has the thread open again"
          (is (contains?
                (set (map :id (thread/open-threads-for-user (user-2 :id))))
                thread-id)))))))

(deftest tag-mention-opens-thread-for-subscribers

  (testing "given 2 users and 2 tags..."
    (let [[user-1 user-2 group] (db/run-txns!
                                  (concat
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "foo@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "quux@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (group/create-group-txn {:id (db/uuid)
                                                             :slug "leanpixel"
                                                             :name "Lean Pixel"})))
          [tag-1] (db/run-txns!
                    (tag/create-tag-txn {:id (db/uuid)
                                         :name "acme1"
                                         :group-id (group :id)}))]
      (db/run-txns!
        (concat
          (group/user-add-to-group-txn (user-2 :id) (group :id))
          (group/user-add-to-group-txn (user-1 :id) (group :id))))

      (testing "given a user subscribed to a tag..."
        (db/run-txns! (tag/user-subscribe-to-tag-txn (user-1 :id) (tag-1 :id)))

        (testing "when a new message mentions the tag..."
          (let [[msg] (db/run-txns!
                        (message/create-message-txn {:id (db/uuid)
                                                     :group-id (group :id)
                                                     :user-id (user-2 :id)
                                                     :thread-id (db/uuid)
                                                     :created-at (java.util.Date.)
                                                     :content "Hello?"
                                                     :mentioned-tag-ids [(tag-1 :id)]}))]

            (testing "then the tag is added to the thread"
              (let [thread (thread/thread-by-id (msg :thread-id))]
                (contains? (set (thread :tag-ids)) (tag-1 :id))))

            (testing "then the user has the thread opened"
              (let [user-threads (map :id (thread/open-threads-for-user (user-1 :id)))]
                (is (contains? (set user-threads) (msg :thread-id)))))

            (testing "then the user is subscribed-to the thread"
              (let [user-threads (thread/subscribed-thread-ids-for-user (user-1 :id))]
                (is (contains? (set user-threads) (msg :thread-id))))
              (let [users (thread/users-subscribed-to-thread (msg :thread-id))]
                (is (contains? (set users) (user-1 :id)))))))))))

(deftest user-mention-subscribes-opens-thread-for-user

  (testing "given 2 users..."
    (let [[user-1 user-2 group] (db/run-txns!
                                  (concat
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "foo@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (user/create-user-txn {:id (db/uuid)
                                                           :email "quux@bar.com"
                                                           :password "foobar"
                                                           :avatar ""})
                                    (group/create-group-txn {:id (db/uuid)
                                                             :slug "leanpixel"
                                                             :name "Lean Pixel"})))
           thread-id (db/uuid)]

      (db/run-txns!
        (concat
          (group/user-add-to-group-txn (user-2 :id) (group :id))
          (group/user-add-to-group-txn (user-1 :id) (group :id))))

      (testing "when user-1 mentions user-2 in a message..."
        (let [[msg] (db/run-txns!
                      (message/create-message-txn {:id (db/uuid)
                                                   :group-id (group :id)
                                                   :user-id (user-1 :id)
                                                   :thread-id thread-id
                                                   :created-at (java.util.Date.)
                                                   :content "Hello?"
                                                   :mentioned-user-ids [(user-2 :id)]}))]

          (testing "then user-2 is added to the thread"
            (is  (= #{(user-2 :id)}
                    (-> (thread/thread-by-id thread-id) :mentioned-ids))))

          (testing "then user-2 can see the thread"
            (is (thread/user-can-see-thread? (user-2 :id) thread-id)))

          (testing "then user-2 is subscribed to the thread"
            (let [users (thread/users-subscribed-to-thread (msg :thread-id))]
              (is (contains? (set users) (user-2 :id)))))

          (testing "then user-2 has the thread opened"
            (is (= [thread-id] (map :id (thread/open-threads-for-user (user-2 :id)))))))))))
