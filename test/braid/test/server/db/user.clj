(ns braid.test.server.db.user
  (:require
    [clojure.string :as string]
    [clojure.test :refer :all]
    [braid.server.db :as db]
    [braid.server.db.user :as user]
    [braid.server.db.group :as group]
    [braid.test.fixtures.db :refer [drop-db]]))

(use-fixtures :each drop-db)

(deftest create-user!
  (let [data {:id (db/uuid)
              :email "foo@bar.com"
              :password "foobar"}
        [user] (db/run-txns! (user/create-user-txn data))
        new-nickname "new-nick"]

    (testing "can check if an email has been used"
      (is (user/email-taken? (:email data)))
      (is (not (user/email-taken? "baz@quux.net"))))

    (testing "create returns a user"
      (is (= #{:avatar :id :nickname :group-ids :email} (set (keys user))))
      (is (= (:id data) (:id user))))

    (testing "can set nickname"
      (is (not (user/nickname-taken? new-nickname)))
      (db/run-txns! (user/set-nickname-txn (user :id) new-nickname))
      (is (user/nickname-taken? new-nickname))
      (is (= (:nickname (user/user-with-email "foo@bar.com"))
             new-nickname)))

    (testing "user email must be unique"
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/run-txns!
                     (user/create-user-txn {:id (db/uuid)
                                            :email (data :email)})))))

    (testing "user nickname must be unique"
      (let [other {:id (db/uuid)
                   :email "baz@quux.com"}]
        (is (some? (db/run-txns! (user/create-user-txn other))))
        (is (thrown? java.util.concurrent.ExecutionException
                     (db/run-txns!
                       (user/set-nickname-txn (other :id) new-nickname))))))))

(deftest create-user
  (testing "id is required"
    (is (thrown? Exception
                 (db/run-txns! (user/create-user-txn {:email "abc@efg.com"})))))

  (testing "id must be unique"
    (let [id (db/uuid)]
      (db/run-txns! (user/create-user-txn {:id id :email "asdfasdf@gmail.com"}))
      (is (thrown? Exception
                   (db/run-txns! (user/create-user-txn {:id id :email "kkkkk@gmail.com"}))))))

  (testing "email is required"
    (is (thrown? Exception
                 (db/run-txns! (user/create-user-txn {:id (db/uuid)})))))

  (testing "email must be unique"
    (let [email "ooo@email.com"]
      (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                            :email email}))
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                        :email email}))))))

  (testing "nickname is set based on email"
    (let [nickname "billy"
          [user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email (str nickname "@bar.com")}))]

      (is (= nickname (user :nickname)))))

  (testing "nickname is slugified"
    (let [raw-nickname "   !A-a "
          safe-nickname "a-a"
          email (str raw-nickname "@bas.com")
          [user-3] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                        :email email}))]
      (is (= safe-nickname (:nickname (user/user-with-email email))))))

  (testing "if email-based nickname is not unique, a number is appended"
    (let [nickname "xxxxx"]
      (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                           :email (str nickname "@bar.com")}))
      (let [[user-2] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                          :email (str nickname "@quux.com")}))]
        (is (re-matches (re-pattern (str "^" nickname ".+")) (:nickname user-2) )))))

  (testing "avatar is set to be a gravatar"
    (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "zxc@example.com"}))]
      (is (string/includes? (:avatar user) "gravatar")))) )

(deftest email-taken?
  (let [data {:id (db/uuid)
              :email "foo@bar.com"
              :password "foobar"
              :avatar "http://www.foobar.com/1.jpg"}
        [user] (db/run-txns! (user/create-user-txn data))]

    (testing "can check if an email has been used"
      (is (user/email-taken? (:email data)))
      (is (not (user/email-taken? "baz@quux.net"))))))

(deftest set-nickname
  (let [nickname "ol-fooy"]

    (testing "can set nickname"
      (let [email "foo@bar.com"
            [user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                        :email "foo@bar.com"}))]
        (db/run-txns! (user/set-nickname-txn (user-1 :id) nickname))
        (is (= nickname
               (:nickname (user/user-with-email email))))))

    (testing "fails when not unique"
      (let [[user-2] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                        :email "baz@bas.com"}))]
        (is (thrown? java.util.concurrent.ExecutionException
                     @(db/run-txns! (user/set-nickname-txn (user-2 :id) nickname))))))

    (testing "nickname is slugified"
      (let [raw-nickname "   @A-a "
            safe-nickname "a-a"
            email "asdf@bas.com"
            [user-3] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                        :email email}))]
        (db/run-txns! (user/set-nickname-txn (user-3 :id) raw-nickname))
        (is (= safe-nickname (:nickname (user/user-with-email email))))))))

(deftest nickname-taken?
  (let [nickname "foo"]
    (testing "returns false when nickname not taken"
      (is (= false (user/nickname-taken? nickname))))

    (testing "returns true when nickname is taken"
      (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                           :email (str nickname "@bar.com")}))
      (is (= true (user/nickname-taken? nickname))))))

(deftest user-with-email
  (let [id (db/uuid)
        data {:email "foo@bar.com"
              :id id}]

    (testing "returns nil when no matching user"
      (is (nil? (user/user-with-email (data :email)))))

    (testing "returns user when matching user"
      (db/run-txns! (user/create-user-txn data))
      (is (= id (:id (user/user-with-email (data :email))))))))

(defn partial-user [u]
  (select-keys u [:id :nickname :avatar]))

(deftest fetch-users
  (let [user-1-data {:id (db/uuid)
                     :email "foo@bar.com"
                     :password "foobar"
                     :avatar "http://www.foobar.com/1.jpg"}
        [user-1] (db/run-txns! (user/create-user-txn user-1-data))
        user-2-data  {:id (db/uuid)
                      :email "bar@baz.com"
                      :password "barbaz"
                      :avatar "http://www.barbaz.com/1.jpg"}
        [user-2] (db/run-txns! (user/create-user-txn user-2-data))
        [group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "aoeu" :name "aoeu"}))
        _ (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group :id)))
        _ (db/run-txns! (group/user-add-to-group-txn (user-2 :id) (group :id)))
        users (user/users-for-user (user-1 :id))]
    (testing "users-for-user"
      (testing "returns all users"
      (is (= (set (map partial-user users))
             (set (map partial-user [user-1 user-2]))))))))

(deftest only-see-users-in-group
  (let [[group-1] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "g1" :name "g1"}))
        [group-2] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "g2" :name "g2"}))
        [user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "foo@bar.com"
                                                      :password "foobar"
                                                      :avatar "http://www.foobar.com/1.jpg"}))
        [user-2] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "bar@baz.com"
                                                      :password "barbaz"
                                                      :avatar "http://www.barbaz.com/1.jpg"}))
        [user-3] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "quux@baz.com"
                                                      :password "barbaz"
                                                      :avatar "http://www.barbaz.com/2.jpg"}))]
    (is (not (group/user-in-group? (user-1 :id) (group-1 :id))))
    (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group-1 :id)))
    (is (group/user-in-group? (user-1 :id) (group-1 :id)))
    (db/run-txns! (group/user-add-to-group-txn (user-2 :id) (group-1 :id)))
    (db/run-txns! (group/user-add-to-group-txn (user-2 :id) (group-2 :id)))
    (db/run-txns! (group/user-add-to-group-txn (user-3 :id) (group-2 :id)))
    (is (not (group/user-in-group? (user-1 :id) (group-2 :id))))
    (is (= (set (map partial-user [user-1 user-2]))
           (set (map partial-user (user/users-for-user (user-1 :id))))))
    (is (= (set (map partial-user [user-1 user-2 user-3]))
           (set (map partial-user (user/users-for-user (user-2 :id))))))
    (is (= (set (map partial-user [user-2 user-3]))
           (set (map partial-user (user/users-for-user (user-3 :id))))))
    (is (user/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (user/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (user/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (user/user-visible-to-user? (user-3 :id) (user-2 :id)))
    (db/run-txns! (group/user-leave-group-txn (user-1 :id) (group-1 :id)))
    (is (not (group/user-in-group? (user-1 :id) (group-1 :id))))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-2 :id))))))

(deftest authenticate-user
  (let [user-id (db/uuid)
        password "foobar"
        user {:id user-id
              :email "fOo@bar.com"}
        _ (db/run-txns! (user/create-user-txn user))
        _ (db/run-txns! (user/set-user-password-txn user-id password))]

    (testing "returns user-id when email+password matches"
      (is (= user-id (user/authenticate-user (user :email) password))))

    (testing "email is case-insensitive"
      (is (= user-id
             (user/authenticate-user "Foo@bar.com" password)
             (user/authenticate-user "foo@bar.com" password)
             (user/authenticate-user "FOO@BAR.COM" password))))

    (testing "returns nil when email+password wrong"
      (is (nil? (user/authenticate-user (user :email) "zzz"))))))
