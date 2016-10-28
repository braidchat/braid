(ns braid.test.server.db.user
  (:require
    [clojure.test :refer :all]
    [braid.server.conf :as conf]
    [braid.server.db :as db]
    [braid.test.server.fixtures :as fixtures]))

(use-fixtures :each fixtures/drop-db)


(deftest create-user
  (testing "id is required"
    (is (thrown? Exception
                 (db/create-user! {:email "abc@efg.com"}))))

  (testing "id must be unique"
    (let [id (db/uuid)]
      (db/create-user! {:id id :email "asdfasdf@gmail.com"})
      (is (thrown? Exception
                   (db/create-user! {:id id :email "kkkkk@gmail.com"})))))

  (testing "email is required"
    (is (thrown? Exception
                 (db/create-user! {:id (db/uuid)}))))

  (testing "email must be unique"
    (let [email "ooo@email.com"]
      (db/create-user! {:id (db/uuid)
                        :email email})
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/create-user! {:id (db/uuid)
                                     :email email})))))

  (testing "nickname is set based on email"
    (let [nickname "billy"
          user (db/create-user! {:id (db/uuid)
                                 :email (str nickname "@bar.com")})]

      (is (= nickname (user :nickname)))))

  (testing "nickname is slugified"
    (let [raw-nickname "   !A-a "
          safe-nickname "a-a"
          email (str raw-nickname "@bas.com")
          user-3 (db/create-user! {:id (db/uuid)
                                   :email email})]
      (is (= safe-nickname (:nickname (db/user-with-email email))))))

  (testing "user nickname (as generated from email) must be unique"
    (let [nickname "xxxxx"]
      (db/create-user! {:id (db/uuid)
                        :email (str nickname "@bar.com")})
      (is (thrown? java.util.concurrent.ExecutionException
                   (db/create-user! {:id (db/uuid)
                                     :email (str nickname "@quux.com")})))))

  (testing "create returns a user with appropriate fields"
    (let [user (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"})]
      (is (= #{:id :group-ids :avatar :nickname} (set (keys user)))))))

(deftest email-taken?
  (let [data {:id (db/uuid)
              :email "foo@bar.com"
              :password "foobar"
              :avatar "http://www.foobar.com/1.jpg"}
        user (db/create-user! data)]

    (testing "can check if an email has been used"
      (is (db/email-taken? (:email data)))
      (is (not (db/email-taken? "baz@quux.net"))))))

(deftest set-nickname
  (let [nickname "ol-fooy"]

    (testing "can set nickname"
      (let [email "foo@bar.com"
            user-1 (db/create-user! {:id (db/uuid)
                                     :email "foo@bar.com"})]
        (db/set-nickname! (user-1 :id) nickname)
        (is (= nickname
               (:nickname (db/user-with-email email))))))

    (testing "fails when not unique"
      (let [user-2 (db/create-user! {:id (db/uuid)
                                     :email "baz@bas.com"})]
        (is (thrown? java.util.concurrent.ExecutionException
                     @(db/set-nickname! (user-2 :id) nickname)))))

    (testing "nickname is slugified"
      (let [raw-nickname "   @A-a "
            safe-nickname "a-a"
            email "asdf@bas.com"
            user-3 (db/create-user! {:id (db/uuid)
                                     :email email})]
        (db/set-nickname! (user-3 :id) raw-nickname)
        (is (= safe-nickname (:nickname (db/user-with-email email))))))))

(deftest nickname-taken?
  (let [nickname "foo"]
    (testing "returns false when nickname not taken"
      (is (= false (db/nickname-taken? nickname))))

    (testing "returns true when nickname is taken"
      (db/create-user! {:id (db/uuid)
                        :email (str nickname "@bar.com")})
      (is (= true (db/nickname-taken? nickname))))))

(deftest user-with-email
  (let [id (db/uuid)
        data {:email "foo@bar.com"
              :id id}]

    (testing "returns nil when no matching user"
      (is (nil? (db/user-with-email (data :email)))))

    (testing "returns user when matching user"
      (db/create-user! data)
      (is (= id (:id (db/user-with-email (data :email))))))))

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
        group (db/create-group! {:id (db/uuid) :slug "aoeu" :name "aoeu"})
        _ (db/user-add-to-group! (user-1 :id) (group :id))
        _ (db/user-add-to-group! (user-2 :id) (group :id))
        users (db/users-for-user (user-1 :id))]
    (testing "users-for-user"
      (testing "returns all users"
      (is (= (set (map (fn [u] (dissoc u :group-ids)) users))
             (set (map (fn [u] (dissoc u :group-ids)) [user-1 user-2]))))))

    ))

(deftest only-see-users-in-group
  (let [group-1 (db/create-group! {:id (db/uuid) :slug "g1" :name "g1"})
        group-2 (db/create-group! {:id (db/uuid) :slug "g2" :name "g2"})
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
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/users-for-user (user-1 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/users-for-user (user-2 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/users-for-user (user-3 :id))))))
    (is (db/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (db/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (db/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (db/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (db/user-visible-to-user? (user-3 :id) (user-2 :id)))
    (db/user-leave-group! (user-1 :id) (group-1 :id))
    (is (not (db/user-in-group? (user-1 :id) (group-1 :id))))
    (is (not (db/user-visible-to-user? (user-1 :id) (user-2 :id))))))

(deftest authenticate-user
  (let [user-id (db/uuid)
        password "foobar"
        user {:id user-id
              :email "fOo@bar.com"}
        _ (db/create-user! user)
        _ (db/set-user-password! user-id password)]

    (testing "returns user-id when email+password matches"
      (is (= user-id (db/authenticate-user (user :email) password))))

    (testing "email is case-insensitive"
      (is (= user-id
             (db/authenticate-user "Foo@bar.com" password)
             (db/authenticate-user "foo@bar.com" password)
             (db/authenticate-user "FOO@BAR.COM" password))))

    (testing "returns nil when email+password wrong"
      (is (nil? (db/authenticate-user (user :email) "zzz"))))))
