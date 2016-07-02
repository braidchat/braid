(ns braid.test.server.new-message
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [braid.server.conf :as conf]
            [braid.server.db :as db]))


(use-fixtures :each
              (fn [t]
                (-> (mount/only #{#'conf/config #'db/conn})
                    (mount/swap {#'conf/config
                                 {:db-url "datomic:mem://chat-test"}})
                    (mount/start))
                (t)
                (datomic.api/delete-database (conf/config :db-url))
                (mount/stop)))


(deftest create-message
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        thread-id (db/uuid)]

    (testing "new messages can create a thread"
      (let [group (db/create-group! {:id (db/uuid) :name "group"})
            message-data {:id (db/uuid)
                          :group-id (group :id)
                          :user-id (user-1 :id)
                          :thread-id thread-id
                          :created-at (java.util.Date.)
                          :content "Hello?"}
            message (db/create-message! message-data)]

        (testing "returns message"
          (is (= (dissoc message-data :group-id) message)))

        (testing "user has thread open"
          (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) thread-id)))

        (testing "user has thread subscribed"
          (is (contains? (set (db/subscribed-thread-ids-for-user (user-1 :id))) thread-id)))))

    (testing "new message can add to an existing thread"
      (let [group (db/create-group! {:id (db/uuid) :name "group2"})
            message-2-data {:id (db/uuid)
                            :group-id (group :id)
                            :user-id (user-1 :id)
                            :thread-id thread-id
                            :created-at (java.util.Date.)
                            :content "Goodbye."}
            message-2 (db/create-message! message-2-data)]

        (testing "returns message"
          (is (= (dissoc message-2-data :group-id) message-2)))

        (testing "user has thread open"
          (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) thread-id)))

        (testing "user has thread subscribed"
          (is (contains? (set (db/subscribed-thread-ids-for-user (user-1 :id))) thread-id)))))))

(deftest new-message-opens-and-subscribes

  (testing "given a user"
    (let [user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          group (db/create-group! {:id (db/uuid) :name "group"})]

      (testing "when the user sends a new message"
        (let [thread-id (db/uuid)
              _ (db/create-message! {:id (db/uuid)
                                     :group-id (group :id)
                                     :user-id (user-1 :id)
                                     :thread-id thread-id
                                     :created-at (java.util.Date.)
                                     :content "Hello?"})]

          (testing "then the user is subscribed to the thread"
            (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) thread-id)))

          (testing "then the user has the thread open"
            (is (contains? (set (db/subscribed-thread-ids-for-user (user-1 :id))) thread-id))))))))

(deftest new-message-opens-thread

  (testing "given a thread with 2 participants"
    (let [user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "quux@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          group (db/create-group! {:id (db/uuid) :name "group"})
          thread-id (db/uuid)
          message-1 (db/create-message! {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id thread-id
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})
          message-2 (db/create-message! {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-2 :id)
                                         :thread-id thread-id
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})]

      (testing "when user-2 hides the thread"
        (db/user-hide-thread! (user-2 :id) thread-id)

        (testing "then user-2 no longer has the thread open"
          (is (not (contains? (set (map :id (db/open-threads-for-user (user-2 :id)))) thread-id)))))

      (testing "when user-1 sends another message in the thread"
        (db/create-message! {:id (db/uuid)
                             :group-id (group :id)
                             :user-id (user-1 :id)
                             :thread-id thread-id
                             :created-at (java.util.Date.)
                             :content "Hello?"})

        (testing "then user-2 has the thread open again"
          (is (contains? (set (map :id (db/open-threads-for-user (user-2 :id)))) thread-id)))))))

(deftest tag-mention-opens-thread-for-subscribers

  (testing "given 2 users and 2 tags..."
    (let [group (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
          tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group :id)})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          _ (db/user-add-to-group! (user-1 :id) (group :id))
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "quux@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          _ (db/user-add-to-group! (user-2 :id) (group :id))]

      (testing "given a user subscribed to a tag..."
        (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))

        (testing "when a new message mentions the tag..."
          (let [msg (db/create-message! {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-2 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"
                                         :mentioned-tag-ids [(tag-1 :id)]})]

            (testing "then the tag is added to the thread"
              (let [thread (db/thread-by-id (msg :thread-id))]
                (contains? (set (thread :tag-ids)) (tag-1 :id))))

            (testing "then the user has the thread opened"
              (let [user-threads (map :id (db/open-threads-for-user (user-1 :id)))]
                (is (contains? (set user-threads) (msg :thread-id)))))

            (testing "then the user is subscribed-to the thread"
              (let [user-threads (db/subscribed-thread-ids-for-user (user-1 :id))]
                (is (contains? (set user-threads) (msg :thread-id))))
              (let [users (db/users-subscribed-to-thread (msg :thread-id))]
                (is (contains? (set users) (user-1 :id)))))))))))

(deftest user-mention-subscribes-opens-thread-for-user

  (testing "given 2 users..."
    (let [group (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          _ (db/user-add-to-group! (user-1 :id) (group :id))
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "quux@bar.com"
                                   :password "foobar"
                                   :avatar ""})
          _ (db/user-add-to-group! (user-2 :id) (group :id))
          thread-id (db/uuid)]

      (testing "when user-1 mentions user-2 in a message..."
        (let [msg (db/create-message! {:id (db/uuid)
                                       :group-id (group :id)
                                       :user-id (user-1 :id)
                                       :thread-id thread-id
                                       :created-at (java.util.Date.)
                                       :content "Hello?"
                                       :mentioned-user-ids [(user-2 :id)]})]

          (testing "then user-2 is added to the thread"
            (is  (= #{(user-2 :id)}
                    (-> (db/thread-by-id thread-id) :mentioned-ids))))

          (testing "then user-2 can see the thread"
            (is (db/user-can-see-thread? (user-2 :id) thread-id)))

          (testing "then user-2 is subscribed to the thread"
            (let [users (db/users-subscribed-to-thread (msg :thread-id))]
              (is (contains? (set users) (user-2 :id)))))

          (testing "then user-2 has the thread opened"
            (is (= [thread-id] (map :id (db/open-threads-for-user (user-2 :id)))))))))))
