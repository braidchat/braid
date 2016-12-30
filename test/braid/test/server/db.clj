(ns braid.test.server.db
  (:require [clojure.test :refer :all]
            [clojure.set :refer [rename-keys]]
            [mount.core :as mount]
            [braid.test.server.test-utils :refer [fetch-messages]]
            [braid.server.conf :as conf]
            [braid.server.db :as db]
            [braid.server.db.bot :as bot]
            [braid.server.db.group :as group]
            [braid.server.db.invitation :as invitation]
            [braid.server.db.message :as message]
            [braid.server.db.tag :as tag]
            [braid.server.db.thread :as thread]
            [braid.server.db.user :as user]
            [braid.common.schema :as schema]
            [braid.server.search :as search]))


(use-fixtures :each
              (fn [t]
                (-> (mount/only #{#'conf/config #'db/conn})
                    (mount/swap {#'conf/config
                                 {:db-url "datomic:mem://chat-test"}})
                    (mount/start))
                (t)
                (datomic.api/delete-database (conf/config :db-url))
                (mount/stop)))


(deftest create-user
  (let [data {:id (db/uuid)
              :email "foo@bar.com"
              :password "foobar"
              :avatar "http://www.foobar.com/1.jpg"}
        user (first (db/run-txns! (user/create-user-txn data)))]
    (testing "can check if an email has been used"
      (is (user/email-taken? (:email data)))
      (is (not (user/email-taken? "baz@quux.net"))))
    (testing "create returns a user"
      (is (= (dissoc user :group-ids)
             (-> data
                 (dissoc :password :email)
                 (assoc :nickname "foo")))))
    (testing "can set nickname"
      (is (not (user/nickname-taken? "ol' fooy")))
      (db/run-txns! (user/set-nickname-txn (user :id) "ol' fooy"))
      (is (user/nickname-taken? "ol' fooy"))
      (is (= (-> (user/user-with-email "foo@bar.com")
                 (dissoc :group-ids))
             (-> data
                 (dissoc :password :email)
                 (assoc :nickname "ol' fooy")))))

    (testing "user email must be unique"
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/run-txns!
                     (user/create-user-txn {:id (db/uuid)
                                            :email (data :email)
                                            :password "zzz"
                                            :avatar "http://zzz.com/2.jpg"})))))
    (testing "user nickname must be unique"
      (let [other {:id (db/uuid)
                   :email "baz@quux.com"
                   :password "foobar"
                   :avatar "foo@bar.com"}]
        (is (some? (db/run-txns! (user/create-user-txn other))))
        (is (thrown? java.util.concurrent.ExecutionException
                     (db/run-txns!
                       (user/set-nickname-txn (other :id)  "ol' fooy"))))))))

(deftest fetch-users
  (let [user-1-data {:id (db/uuid)
                     :email "foo@bar.com"
                     :password "foobar"
                     :avatar "http://www.foobar.com/1.jpg"}
        user-2-data  {:id (db/uuid)
                      :email "bar@baz.com"
                      :password "barbaz"
                      :avatar "http://www.barbaz.com/1.jpg"}
        [user-1 user-2] (db/run-txns!
                          (concat (user/create-user-txn user-1-data)
                                  (user/create-user-txn user-2-data)))
        group (group/create-group! {:id (db/uuid) :name "aoeu"})
        _ (db/run-txns!
            (concat
              (group/user-add-to-group-txn (user-1 :id) (group :id))
              (group/user-add-to-group-txn (user-2 :id) (group :id))))
        users (user/users-for-user (user-1 :id))]
    (testing "returns all users"
      (is (= (set (map (fn [u] (dissoc u :group-ids)) users))
             (set (map (fn [u] (dissoc u :group-ids)) [user-1 user-2])))))
    (testing "get user by email"
      (is (= (dissoc user-1 :group-ids)
             (dissoc (user/user-with-email (user-1-data :email)) :group-ids)))
      (is (nil? (user/user-with-email "zzzzz@zzzzzz.ru"))))))

(deftest only-see-users-in-group
  (let [group-1 (group/create-group! {:id (db/uuid) :name "g1"})
        group-2 (group/create-group! {:id (db/uuid) :name "g2"})
        [user-1 user-2 user-3] (db/run-txns!
                                 (concat
                                   (user/create-user-txn {:id (db/uuid)
                                                          :email "foo@bar.com"
                                                          :password "foobar"
                                                          :avatar "http://www.foobar.com/1.jpg"})
                                   (user/create-user-txn {:id (db/uuid)
                                                          :email "bar@baz.com"
                                                          :password "barbaz"
                                                          :avatar "http://www.barbaz.com/1.jpg"})
                                   (user/create-user-txn {:id (db/uuid)
                                                          :email "quux@baz.com"
                                                          :password "barbaz"
                                                          :avatar "http://www.barbaz.com/2.jpg"})))]
    (is (not (group/user-in-group? (user-1 :id) (group-1 :id))))
    (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group-1 :id)))
    (is (group/user-in-group? (user-1 :id) (group-1 :id)))
    (db/run-txns!
      (concat
        (group/user-add-to-group-txn (user-2 :id) (group-1 :id))
        (group/user-add-to-group-txn (user-2 :id) (group-2 :id))
        (group/user-add-to-group-txn (user-3 :id) (group-2 :id))))
    (is (not (group/user-in-group? (user-1 :id) (group-2 :id))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2]))
           (set (map (fn [u] (dissoc u :group-ids))
                (user/users-for-user (user-1 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (user/users-for-user (user-2 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (user/users-for-user (user-3 :id))))))
    (is (user/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (user/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (user/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (user/user-visible-to-user? (user-3 :id) (user-2 :id)))
    (db/run-txns! (group/user-leave-group-txn (user-1 :id) (group-1 :id)))
    (is (not (group/user-in-group? (user-1 :id) (group-1 :id))))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-2 :id))))))

(deftest authenticate-user
  (let [user-1-data {:id (db/uuid)
                     :email "fOo@bar.com"
                     :password "foobar"
                     :avatar ""}]
    (db/run-txns! (user/create-user-txn user-1-data))
    (testing "returns user-id when email+password matches"
      (is (= (:id user-1-data)
             (user/authenticate-user (user-1-data :email) (user-1-data :password)))))

    (testing "email is case-insensitive"
      (is (= (:id user-1-data)
             (user/authenticate-user "Foo@bar.com" (user-1-data :password))
             (user/authenticate-user "foo@bar.com" (user-1-data :password))
             (user/authenticate-user "FOO@BAR.COM" (user-1-data :password)))))

    (testing "returns nil when email+password wrong"
      (is (nil? (user/authenticate-user (user-1-data :email) "zzz"))))))

(deftest create-group
  (let [data {:id (db/uuid)
              :name "Lean Pixel"}
        group (group/create-group! data)
        user-id (db/uuid)
        user-2-id (db/uuid)]
    (testing "can create a group"
      (is (= group (assoc data :admins #{} :intro nil :avatar nil
                     :public? false :bots #{}))))
    (testing "can set group intro"
      (db/run-txns! (group/group-set-txn (group :id) :intro "the intro"))
      (is (= (group/group-by-id (group :id))
             (assoc data :admins #{} :intro "the intro" :avatar nil
               :public? false :bots #{}))))
    (testing "can add a user to the group"
      (let [[user] (db/run-txns! (user/create-user-txn
                                   {:id user-id
                                    :email "foo@bar.com"
                                    :password "foobar"
                                    :avatar "http://www.foobar.com/1.jpg"}))]
        (is (= #{} (group/user-groups (user :id))))
        (is (= #{} (group/group-users (group :id))))
        (db/run-txns! (group/user-add-to-group-txn (user :id) (group :id)))
        (is (= #{(assoc data :admins #{} :intro "the intro" :avatar nil
                   :public? false :bots #{})}
               (group/user-groups (user :id))))
        (is (= #{(dissoc user :group-ids)}
               (set (map (fn [u] (dissoc user :group-ids))
                    (group/group-users (group :id))))))))
    (testing "groups have no admins by default"
      (is (empty? (:admins (group/group-by-id (group :id))))))
    (testing "Can add admin"
      (db/run-txns! (group/user-make-group-admin-txn user-id (group :id)))
      (is (= #{user-id} (:admins (group/group-by-id (group :id)))))
      (testing "and another admin"
        (db/run-txns!
          (user/create-user-txn
            {:id user-2-id
             :email "bar@baz.com"
             :password "foobar"
             :avatar "http://www.foobar.com/1.jpg"}))
        (db/run-txns! (group/user-make-group-admin-txn user-2-id (group :id)))
        (is (= #{user-id user-2-id} (:admins (group/group-by-id (group :id)))))))
    (testing "multiple groups, admin statuses"
      (let [group-2 (group/create-group! {:id (db/uuid)
                                                  :name "another group"})
            group-3 (group/create-group! {:id (db/uuid)
                                                  :name "third group"})]

        (db/run-txns!
          (concat
            (group/user-add-to-group-txn user-id (group-2 :id))
            (group/user-make-group-admin-txn user-2-id (group-2 :id))

            (group/user-make-group-admin-txn user-id (group-3 :id))
            (group/user-add-to-group-txn user-2-id (group-3 :id))))

        (is (group/user-in-group? user-id (group-2 :id)))
        (is (group/user-in-group? user-id (group-3 :id)))

        (is (group/user-in-group? user-2-id (group-2 :id)))
        (is (group/user-in-group? user-2-id (group-3 :id)))

        (is (= #{(group :id) (group-2 :id) (group-3 :id)}
               (into #{} (map :id) (group/user-groups user-id))
               (into #{} (map :id) (group/user-groups user-2-id)))
            "Both users are in all the groups")

        (is (= #{user-2-id} (:admins (group/group-by-id (group-2 :id)))))
        (is (= #{user-id} (:admins (group/group-by-id (group-3 :id)))))

        (is (group/user-is-group-admin? user-id (group :id)))
        (is (not (group/user-is-group-admin? user-id (group-2 :id))))
        (is (group/user-is-group-admin? user-id (group-3 :id)))

        (is (group/user-is-group-admin? user-2-id (group :id)))
        (is (group/user-is-group-admin? user-2-id (group-2 :id)))
        (is (not (group/user-is-group-admin? user-2-id (group-3 :id))))

        ))))

(deftest fetch-messages-test
  (let [group (group/create-group! {:id (db/uuid) :name "group"})
        [user-1] (db/run-txns!
                   (user/create-user-txn {:id (db/uuid)
                                          :email "foo@bar.com"
                                          :password "foobar"
                                          :avatar "http://www.foobar.com/1.jpg"}))
        thread-1-id (db/uuid)
        message-1-data {:id (db/uuid)
                        :group-id (group :id)
                        :user-id (user-1 :id)
                        :thread-id thread-1-id
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-1 (message/create-message! message-1-data)
        message-2-data {:id (db/uuid)
                        :group-id (group :id)
                        :user-id (user-1 :id)
                        :thread-id (message-1 :thread-id)
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-2 (message/create-message! message-2-data)
        messages (fetch-messages)]
    (testing "fetch-messages returns all messages"
      (is (= (set messages) #{message-1 message-2})))
    (testing "Can retrieve threads"
      (is (= (thread/thread-by-id thread-1-id)
             {:id thread-1-id
              :group-id (group :id)
              :messages (map #(dissoc % :thread-id :group-id)
                             [message-1-data message-2-data])
              :tag-ids #{} :mentioned-ids #{}}))
      (let [thread-2-id (db/uuid)
            message-3-data {:id (db/uuid)
                            :group-id (group :id)
                            :user-id (user-1 :id)
                            :thread-id thread-2-id
                            :created-at (java.util.Date.)
                            :content "Blurrp"}
            message-3 (message/create-message! message-3-data)]
        (is (= (thread/threads-by-id [thread-1-id thread-2-id])
               [{:id thread-1-id
                 :group-id (group :id)
                 :messages (map #(dissoc % :thread-id :group-id)
                                [message-1-data message-2-data])
                 :tag-ids #{} :mentioned-ids #{}}
                {:id thread-2-id
                 :group-id (group :id)
                 :messages (map #(dissoc % :thread-id :group-id)
                                [message-3-data])
                 :tag-ids #{} :mentioned-ids #{}}]))))))

(deftest user-hide-thread
  (let [group (group/create-group! {:id (db/uuid) :name "group"})
        [user-1] (db/run-txns!
                   (user/create-user-txn {:id (db/uuid)
                                          :email "foo@bar.com"
                                          :password "foobar"
                                          :avatar ""}))
        message-1 (message/create-message! {:id (db/uuid)
                                                    :group-id (group :id)
                                                    :user-id (user-1 :id)
                                                    :thread-id (db/uuid)
                                                    :created-at (java.util.Date.)
                                                    :content "Hello?"})
        message-1-b (message/create-message! {:id (db/uuid)
                                                      :group-id (group :id)
                                                      :user-id (user-1 :id)
                                                      :thread-id (db/uuid)
                                                      :created-at (java.util.Date.)
                                                      :content "Hello?"})
        message-2 (message/create-message! {:id (db/uuid)
                                                    :group-id (group :id)
                                                    :user-id (user-1 :id)
                                                    :thread-id (db/uuid)
                                                    :created-at (java.util.Date.)
                                                    :content "Hello?"})]
    (testing "thread 1 is open"
      (is (contains? (set (map :id (thread/open-threads-for-user (user-1 :id))))
                     (message-1 :thread-id))))
    (testing "thread 2 is open"
      (is (contains?
            (set (map :id (thread/open-threads-for-user (user-1 :id))))
            (message-2 :thread-id))))
    (testing "user can hide thread"
      (db/run-txns! (thread/user-hide-thread-txn (user-1 :id) (message-1 :thread-id)))
      (is (not (contains? (set (map :id (thread/open-threads-for-user (user-1 :id))))
                          (message-1 :thread-id)))))
    (testing "user can hide thread"
      (db/run-txns! (thread/user-hide-thread-txn (user-1 :id) (message-2 :thread-id)))
      (is (not (contains?
                 (set (map :id (thread/open-threads-for-user (user-1 :id))))
                 (message-2 :thread-id)))))
    (testing "thread is re-opened when it gets another message"
      (let [[user-2] (db/run-txns!
                       (user/create-user-txn {:id (db/uuid) :email "bar@baz.com"
                                              :password "foobar" :avatar ""}))]
        (message/create-message! {:id (db/uuid)
                                  :group-id (group :id)
                                  :user-id (user-2 :id)
                                  :thread-id (message-1 :thread-id)
                                  :created-at (java.util.Date.)
                                  :content "wake up"})
        (is (contains?
              (set (map :id (thread/open-threads-for-user (user-1 :id))))
              (message-1 :thread-id)))
        (testing "unless the user has unsubscribed from the thread"
          (db/run-txns!
            (thread/user-unsubscribe-from-thread-txn (user-1 :id)
                                                     (message-2 :thread-id)))
          (message/create-message! {:id (db/uuid)
                                            :group-id (group :id)
                                            :user-id (user-2 :id)
                                            :thread-id (message-2 :thread-id)
                                            :created-at (java.util.Date.)
                                            :content "wake up"})
          (is (not (contains?
                     (set (map :id (thread/open-threads-for-user (user-1 :id))))
                     (message-2 :thread-id))))
          (testing "but they get re-subscribed if they get mentioned/another tag is added"
            (message/create-message! {:id (db/uuid)
                                              :group-id (group :id)
                                              :user-id (user-2 :id)
                                              :thread-id (message-2 :thread-id)
                                              :created-at (java.util.Date.)
                                              :content "wake up"
                                              :mentioned-user-ids [(user-1 :id)]})
            (is (contains?
                  (set (map :id (thread/open-threads-for-user (user-1 :id))))
                  (message-2 :thread-id)))))))))

(deftest user-thread-visibility
  (let [[user-1 user-2 user-3] (db/run-txns!
                                 (concat (user/create-user-txn {:id (db/uuid)
                                                                :email "foo@bar.com"
                                                                :password "foobar"
                                                                :avatar ""})
                                         (user/create-user-txn {:id (db/uuid)
                                                                :email "quux@bar.com"
                                                                :password "foobar"
                                                                :avatar ""})
                                         (user/create-user-txn {:id (db/uuid)
                                                                :email "qaax@bar.com"
                                                                :password "foobar"
                                                                :avatar ""})))
        group-1 (group/create-group! {:id (db/uuid)
                                              :name "Lean Pixel"})
        group-2 (group/create-group! {:id (db/uuid)
                                              :name "Penyo Pal"})
        tag-1 (tag/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (tag/create-tag! {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})

        thread-1-id (db/uuid)
        thread-2-id (db/uuid)]

    (testing "everyone can see threads because they haven't been created"
      (is (thread/user-can-see-thread? (user-1 :id) thread-1-id))
      (is (thread/user-can-see-thread? (user-2 :id) thread-1-id))
      (is (thread/user-can-see-thread? (user-3 :id) thread-1-id)))

    (db/run-txns!
      (concat
        (group/user-add-to-group-txn (user-2 :id) (group-1 :id))
        (tag/user-subscribe-to-tag-txn (user-2 :id) (tag-1 :id))
        (group/user-add-to-group-txn (user-3 :id) (group-2 :id))
        (group/user-subscribe-to-group-tags-txn (user-3 :id) (group-2 :id))))
    (message/create-message! {:thread-id thread-1-id :id (db/uuid) :content "zzz"
                              :user-id (user-1 :id) :created-at (java.util.Date.)
                              :mentioned-tag-ids [(tag-1 :id)] :group-id (group-1 :id)})
    (message/create-message! {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                              :user-id (user-2 :id) :created-at (java.util.Date.)
                              :mentioned-tag-ids [(tag-2 :id)] :group-id (group-2 :id)})


    (testing "user 1 can see thread 1 because they created it"
      (is (thread/user-can-see-thread? (user-1 :id) thread-1-id)))
    (testing "user 1 can't see thread 2"
      (is (not (thread/user-can-see-thread? (user-1 :id) thread-2-id))))

    (testing "user 2 can see thread 1 because they've already been subscribed"
      (is (thread/user-can-see-thread? (user-2 :id) thread-1-id)))
    (testing "user 2 can see thread 2 because they created it"
      (is (thread/user-can-see-thread? (user-2 :id) thread-2-id)))

    (testing "user 3 can't see thread 1"
      (is (not (thread/user-can-see-thread? (user-3 :id) thread-1-id))))
    (testing "user 3 can see thread 2 because they have the tag"
      (is (thread/user-can-see-thread? (user-3 :id) thread-2-id))
      (testing "but they can't after leaving"
        (db/run-txns! (group/user-leave-group-txn (user-3 :id) (group-2 :id)))
        (is (not (thread/user-can-see-thread? (user-3 :id) thread-2-id)))))
    (testing "user can leave one group and still see threads in the other"
      (db/run-txns! (group/user-leave-group-txn (user-2 :id) (group-1 :id)))
      (is (not (thread/user-can-see-thread? (user-2 :id) thread-1-id)))
      (is (thread/user-can-see-thread? (user-2 :id) thread-2-id)))))

(deftest user-invite-to-group
  (let [[user-1 user-2] (db/run-txns!
                          (concat
                            (user/create-user-txn {:id (db/uuid)
                                                   :email "foo@bar.com"
                                                   :password "foobar"
                                                   :avatar ""})
                            (user/create-user-txn {:id (db/uuid)
                                                   :email "bar@baz.com"
                                                   :password "foobar"
                                                   :avatar ""})))
        group (group/create-group! {:name "group 1" :id (db/uuid)})]
    (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group :id)))
    (is (empty? (invitation/invites-for-user (user-1 :id))))
    (is (empty? (invitation/invites-for-user (user-2 :id))))
    (let [invite-id (db/uuid)
          invite (invitation/create-invitation!
                   {:id invite-id
                    :inviter-id (user-1 :id)
                    :invitee-email "bar@baz.com"
                    :group-id (group :id)})]
      (is (= invite (invitation/invite-by-id invite-id)))
      (is (seq (invitation/invites-for-user (user-2 :id))))
      (db/run-txns! (invitation/retract-invitation-txn invite-id))
      (is (empty? (invitation/invites-for-user (user-2 :id)))))))

(deftest user-leaving-group
  (let [[user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "foo@bar.com"
                                                      :password "foobar"
                                                      :avatar ""}))
        group (group/create-group! {:id (db/uuid) :name "group 1"})
        thread-id (db/uuid)]
    (message/create-message! {:id (db/uuid) :thread-id thread-id
                              :group-id (group :id) :user-id (user-1 :id)
                              :created-at (java.util.Date.)
                              :content "foobar"
                              :mentioned-user-ids [(user-1 :id)]
                              :mentioned-tag-ids []})
    (testing "user leaving group removes mentions of that user"
      (is (= #{(user-1 :id)}
             (:mentioned-ids (thread/thread-by-id thread-id))))
      (db/run-txns! (group/user-leave-group-txn (user-1 :id) (group :id)))
      (is (empty? (:mentioned-ids (thread/thread-by-id thread-id)))))))

(deftest adding-user-to-group-subscribes-tags
  (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                    :email "foo@bar.com"
                                                    :password "foobar"
                                                    :avatar ""}))
        group (group/create-group! {:name "group" :id (db/uuid)})
        group-tags (doall
                     (map tag/create-tag!
                          [{:id (db/uuid) :name "t1" :group-id (group :id)}
                           {:id (db/uuid) :name "t2" :group-id (group :id)}
                           {:id (db/uuid) :name "t3" :group-id (group :id)}]))]
    (testing "some misc functions"
      (is (= group (group/group-by-id (group :id))))
      (is (= (set group-tags)
             (set (group/group-tags (group :id))))))
    (db/run-txns!
      (concat
        (group/user-add-to-group-txn (user :id) (group :id))
        (group/user-subscribe-to-group-tags-txn (user :id) (group :id))))
    (is (= (set (tag/subscribed-tag-ids-for-user (user :id)))
           (tag/tag-ids-for-user (user :id))
           (set (map :id group-tags))))
    (testing "can remove tags"
      (db/run-txns! (tag/retract-tag-txn (:id (first group-tags))))
      (is (= 2 (count (group/group-tags (group :id))))))))

(deftest user-preferences
  (testing "Can set and retrieve preferences"
    (let [[u] (db/run-txns! (user/create-user-txn
                              {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""}))]
      (is (empty? (user/user-get-preferences (:id u))))
      (db/run-txns! (user/user-set-preference-txn (:id u) :email-frequency :weekly))
      (is (= {:email-frequency :weekly}
             (user/user-get-preferences (:id u))))
      (is (= :weekly (user/user-get-preference (:id u) :email-frequency)))
      (db/run-txns! (user/user-set-preference-txn (:id u) :email-frequency :daily))
      (is (= :daily (user/user-get-preference (:id u) :email-frequency)))
      (db/run-txns! (user/user-set-preference-txn (:id u) :email-frequency :weekly))
      (testing "can search by preferences"
        (let [[u1 u2 u3] (->> (db/run-txns!
                                (concat
                                  (user/create-user-txn {:id (db/uuid)
                                                         :email "foo@baz.com"
                                                         :password "foobar"
                                                         :avatar ""
                                                         :nickname "zzz"})
                                  (user/create-user-txn {:id (db/uuid)
                                                         :email "bar@bar.com"
                                                         :password "foobar"
                                                         :avatar ""})
                                  (user/create-user-txn {:id (db/uuid)
                                                         :email "baz@bar.com"
                                                         :password "foobar"
                                                         :avatar ""})))
                              (map :id))]
          (db/run-txns!
            (concat
              (user/user-set-preference-txn u1 :email-frequency :daily)
              (user/user-set-preference-txn u1 :favourite-color "blue")
              (user/user-set-preference-txn u2 :email-frequency :weekly)
              (user/user-set-preference-txn u2 :favourite-color "blue")))
          (is (= #{u2 (u :id)} (set (user/user-search-preferences
                                      :email-frequency :weekly))))
          (is (= [u1] (user/user-search-preferences
                        :email-frequency :daily)))
          (is (= #{u1 u2} (set (user/user-search-preferences
                                 :favourite-color "blue")))))))))

(deftest bots-test
  (let [g1 (group/create-group! {:name "group 1" :id (db/uuid)})
        g2 (group/create-group! {:name "group 2" :id (db/uuid)})
        g3 (group/create-group! {:name "group 3" :id (db/uuid)})

        [b1 b2 b3] (db/run-txns!
                     (concat
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot1"
                                            :avatar ""
                                            :webhook-url ""
                                            :group-id (g1 :id)})
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot2"
                                            :avatar ""
                                            :webhook-url ""
                                            :group-id (g1 :id)})
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot3"
                                            :avatar ""
                                            :webhook-url ""
                                            :group-id (g2 :id)})))
        bot->display (fn [b] (-> b
                                 (select-keys [:id :name :avatar :user-id])
                                 (rename-keys {:name :nickname})))]

    (is (schema/check-bot! b1))
    (is (schema/check-bot! b2))
    (is (schema/check-bot! b3))
    (testing "can create bots & retrieve by group"
      (is (= #{b1 b2} (bot/bots-in-group (g1 :id))))
      (is (= (into #{} (map bot->display) [b1 b2])
             (:bots (group/group-by-id (g1 :id)))))
      (is (= #{b3} (bot/bots-in-group (g2 :id))))
      (is (= #{} (bot/bots-in-group (g3 :id))))
      (is (= b1 (bot/bot-by-name-in-group "bot1" (g1 :id))))
      (is (nil? (bot/bot-by-name-in-group "bot1" (g3 :id)))))
    (testing "can check bot auth"
      (is (bot/bot-auth? (b1 :id) (b1 :token)))
      (is (bot/bot-auth? (b2 :id) (b2 :token)))
      (is (not (bot/bot-auth? (b2 :id) "Foo")))
      (is (not (bot/bot-auth? (java.util.UUID/randomUUID) (b2 :token)))))))
