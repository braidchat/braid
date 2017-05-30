(ns braid.test.server.db.group
  (:require
    [clojure.test :refer :all]
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.db.user :as user]
    [braid.test.fixtures.db :refer [drop-db]]))

(use-fixtures :each drop-db)

; Unit Tests

(deftest create-group
  (let [data {:id (db/uuid)
              :slug "foobar"
              :name "Foo Bar"}
        [group] (db/run-txns! (group/create-group-txn data))]

    (testing "returns a group with correct fields"
      (is (= group (merge {:id nil
                           :name nil
                           :slug nil
                           :admins #{}
                           :intro nil
                           :avatar nil
                           :public? false
                           :users-count 0
                           :bots #{}}
                          data))))

    (testing "groups have no admins by default"
      (is (empty? (:admins group))))

    (testing "error if slug is not unique"
      (is (thrown-with-msg?  Exception
                            #"unique-conflict"
                            (db/run-txns!
                              (group/create-group-txn {:id (db/uuid)
                                                    :name "Dupe"
                                                    :slug (data :slug)})))))))

(deftest group-with-slug-exists?
  (db/run-txns!
    (group/create-group-txn {:id (db/uuid)
                             :slug "existing-group"
                             :name "Existing Group"}))

  (testing "returns true when a matching group exists"
    (is (= (group/group-with-slug-exists? "existing-group") true)))

  (testing "returns false when a matching group does not exist"
    (is (= (group/group-with-slug-exists? "not-existing-group") false))))


; BDD Tests

(deftest set-group-intro
  (testing "can set an existing group's intro"
    (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "a" :name "a"}))
          intro "the intro"]
      (db/run-txns! (group/group-set-txn (group :id) :intro intro))
      (is (= (:intro (group/group-by-id (group :id))) intro)))))

(deftest add-user-to-group
  (testing "can add a user to a group"
    (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "b" :name "b"}))
          user-id (db/uuid)
          [user] (db/run-txns! (user/create-user-txn {:id user-id
                                                    :email "foo@bar.com"
                                                    :password "foobar"
                                                    :avatar "http://www.foobar.com/1.jpg"}))]
      (is (= #{} (group/user-groups (user :id))))
      (is (= #{} (group/group-users (group :id))))
      (db/run-txns! (group/user-add-to-group-txn (user :id) (group :id)))
      (is (= #{(:id group)}
             (set (map :id (group/user-groups (user :id))))))
      (is (= #{(dissoc user :group-ids)}
             (set (map (fn [u] (dissoc user :group-ids))
                       (group/group-users (group :id)))))))))

(deftest user-group-admin
  (testing "can make a user a group admin"
    (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "c" :name "c"}))
          [user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "foo@bar.com"
                                                      :password "foobar"
                                                      :avatar "http://www.foobar.com/1.jpg"}))
          _ (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group :id)))
          [user-2] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email "bar@baz.com"
                                                      :password "foobar"
                                                      :avatar "http://www.foobar.com/1.jpg"}))
          _ (db/run-txns! (group/user-add-to-group-txn (user-2 :id) (group :id)))]

      (db/run-txns! (group/user-make-group-admin-txn (user-1 :id) (group :id)))

      (is (= #{(user-1 :id)} (:admins (group/group-by-id (group :id)))))

      (db/run-txns! (group/user-make-group-admin-txn (user-2 :id) (group :id)))

      (is (= #{(user-1 :id) (user-2 :id)} (:admins (group/group-by-id (group :id))))))))

(deftest integration-tests
  (testing "multiple groups, admin statuses"
    (let [[user-1] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                       :email "foo@bar.com"
                                                       :password "foobar"
                                                       :avatar "http://www.foobar.com/1.jpg"}))
          [user-2] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                       :email "bar@baz.com"
                                                       :password "foobar"
                                                       :avatar "http://www.foobar.com/1.jpg"}))
          [group-1] (db/run-txns! (group/create-group-txn {:id (db/uuid)
                                                         :slug "third-group"
                                                         :name "third group"}))
          [group-2] (db/run-txns! (group/create-group-txn {:id (db/uuid)
                                                         :slug "another-group"
                                                         :name "another group"}))]

      (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group-2 :id)))
      (db/run-txns! (group/user-make-group-admin-txn (user-2 :id) (group-2 :id)))

      (db/run-txns! (group/user-make-group-admin-txn (user-1 :id) (group-1 :id)))
      (db/run-txns! (group/user-add-to-group-txn (user-2 :id) (group-1 :id)))

      (is (group/user-in-group? (user-1 :id) (group-2 :id)))
      (is (group/user-in-group? (user-1 :id) (group-1 :id)))

      (is (group/user-in-group? (user-2 :id) (group-2 :id)))
      (is (group/user-in-group? (user-2 :id) (group-1 :id)))

      (is (= #{(group-2 :id) (group-1 :id)}
             (into #{} (map :id) (group/user-groups (user-1 :id)))
             (into #{} (map :id) (group/user-groups (user-2 :id))))
          "Both users are in all the groups")

      (is (= #{(user-2 :id)} (:admins (group/group-by-id (group-2 :id)))))
      (is (= #{(user-1 :id)} (:admins (group/group-by-id (group-1 :id)))))

      (is (not (group/user-is-group-admin? (user-1 :id) (group-2 :id))))
      (is (group/user-is-group-admin? (user-1 :id) (group-1 :id)))

      (is (group/user-is-group-admin? (user-2 :id) (group-2 :id)))
      (is (not (group/user-is-group-admin? (user-2 :id) (group-1 :id)))))))
