(ns braid.test.server.db.group
  (:require [clojure.test :refer :all]
            [braid.server.conf :as conf]
            [braid.server.db :as db]
            [braid.test.server.fixtures :as fixtures]))

(use-fixtures :each fixtures/drop-db)

; Unit Tests

(deftest create-group
  (let [data {:id (db/uuid)
              :slug "foobar"
              :name "Foo Bar"}
        group (db/create-group! data)]

    (testing "returns a group with correct fields"
      (is (= group (merge {:id nil
                           :name nil
                           :slug nil
                           :admins #{}
                           :intro nil
                           :avatar nil
                           :public? false
                           :bots #{}}
                          data))))

    (testing "groups have no admins by default"
      (is (empty? (:admins group))))

    (testing "error if slug is not unique"
      (is (thrown-with-msg?  Exception
                            #"unique-conflict"
                            (db/create-group! {:id (db/uuid)
                                               :name "Dupe"
                                               :slug (data :slug)}))))))

(deftest group-with-slug-exists?
  (db/create-group! {:id (db/uuid)
                     :slug "existing-group"
                     :name "Existing Group"})

  (testing "returns true when a matching group exists"
    (is (= (db/group-with-slug-exists? "existing-group") true)))

  (testing "returns false when a matching group does not exist"
    (is (= (db/group-with-slug-exists? "not-existing-group") false))))


; BDD Tests

(deftest set-group-intro
  (testing "can set an existing group's intro"
    (let [group (db/create-group! {:id (db/uuid) :slug "a" :name "a"})
          intro "the intro"]
      (db/group-set! (group :id) :intro intro)
      (is (= (:intro (db/group-by-id (group :id))) intro)))))

(deftest add-user-to-group
  (testing "can add a user to a group"
    (let [group (db/create-group! {:id (db/uuid) :slug "b" :name "b"})
          user-id (db/uuid)
          user (db/create-user! {:id user-id
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})]
      (is (= #{} (db/user-groups (user :id))))
      (is (= #{} (db/group-users (group :id))))
      (db/user-add-to-group! (user :id) (group :id))
      (is (= #{group}
             (db/user-groups (user :id))))
      (is (= #{(dissoc user :group-ids)}
             (set (map (fn [u] (dissoc user :group-ids))
                       (db/group-users (group :id)))))))))

(deftest user-group-admin
  (testing "can make a user a group admin"
    (let [group (db/create-group! {:id (db/uuid) :slug "c" :name "c"})
          user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})
          _ (db/user-add-to-group! (user-1 :id) (group :id))
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "bar@baz.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})
          _ (db/user-add-to-group! (user-2 :id) (group :id))]

      (db/user-make-group-admin! (user-1 :id) (group :id))

      (is (= #{(user-1 :id)} (:admins (db/group-by-id (group :id)))))

      (db/user-make-group-admin! (user-2 :id) (group :id))

      (is (= #{(user-1 :id) (user-2 :id)} (:admins (db/group-by-id (group :id))))))))

(deftest integration-tests
  (testing "multiple groups, admin statuses"
    (let [user-1 (db/create-user! {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})
          user-2 (db/create-user! {:id (db/uuid)
                                   :email "bar@baz.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})
          group-1 (db/create-group! {:id (db/uuid)
                                     :slug "third-group"
                                     :name "third group"})
          group-2 (db/create-group! {:id (db/uuid)
                                     :slug "another-group"
                                     :name "another group"})]

      (db/user-add-to-group! (user-1 :id) (group-2 :id))
      (db/user-make-group-admin! (user-2 :id) (group-2 :id))

      (db/user-make-group-admin! (user-1 :id) (group-1 :id))
      (db/user-add-to-group! (user-2 :id) (group-1 :id))

      (is (db/user-in-group? (user-1 :id) (group-2 :id)))
      (is (db/user-in-group? (user-1 :id) (group-1 :id)))

      (is (db/user-in-group? (user-2 :id) (group-2 :id)))
      (is (db/user-in-group? (user-2 :id) (group-1 :id)))

      (is (= #{(group-2 :id) (group-1 :id)}
             (into #{} (map :id) (db/user-groups (user-1 :id)))
             (into #{} (map :id) (db/user-groups (user-2 :id))))
          "Both users are in all the groups")

      (is (= #{(user-2 :id)} (:admins (db/group-by-id (group-2 :id)))))
      (is (= #{(user-1 :id)} (:admins (db/group-by-id (group-1 :id)))))

      (is (not (db/user-is-group-admin? (user-1 :id) (group-2 :id))))
      (is (db/user-is-group-admin? (user-1 :id) (group-1 :id)))

      (is (db/user-is-group-admin? (user-2 :id) (group-2 :id)))
      (is (not (db/user-is-group-admin? (user-2 :id) (group-1 :id)))))))
