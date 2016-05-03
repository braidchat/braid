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
      (is (= (dissoc user :group-ids)
             (-> data
                 (dissoc :password :email)
                 (assoc :nickname "foo")))))
    (testing "can set nickname"
      (is (not (db/nickname-taken? "ol' fooy")))
      @(db/set-nickname! (user :id) "ol' fooy")
      (is (db/nickname-taken? "ol' fooy"))
      (is (= (-> (db/user-with-email "foo@bar.com")
                 (dissoc :group-ids))
             (-> data
                 (dissoc :password :email)
                 (assoc :nickname "ol' fooy"))))
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
      (is (= (set (map (fn [u] (dissoc u :group-ids)) users))
             (set (map (fn [u] (dissoc u :group-ids)) [user-1 user-2])))))
    (testing "get user by email"
      (is (= (dissoc user-1 :group-ids)
             (dissoc (db/user-with-email (user-1-data :email)) :group-ids)))
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
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/fetch-users-for-user (user-1 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-1 user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/fetch-users-for-user (user-2 :id))))))
    (is (= (set (map (fn [u] (dissoc u :group-ids))
                     [user-2 user-3]))
           (set (map (fn [u] (dissoc u :group-ids))
                (db/fetch-users-for-user (user-3 :id))))))
    (is (db/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (db/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (db/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (db/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (db/user-visible-to-user? (user-3 :id) (user-2 :id)))))

(deftest authenticate-user
  (let [user-1-data {:id (db/uuid)
                     :email "fOo@bar.com"
                     :password "foobar"
                     :avatar ""}
        _ (db/create-user! user-1-data)]

    (testing "returns user-id when email+password matches"
      (is (= (:id user-1-data) (db/authenticate-user (user-1-data :email) (user-1-data :password)))))

    (testing "email is case-insensitive"
      (is (= (:id user-1-data)
             (db/authenticate-user "Foo@bar.com" (user-1-data :password))
             (db/authenticate-user "foo@bar.com" (user-1-data :password))
             (db/authenticate-user "FOO@BAR.COM" (user-1-data :password)))))

    (testing "returns nil when email+password wrong"
      (is (nil? (db/authenticate-user (user-1-data :email) "zzz"))))))

(deftest create-group
  (let [data {:id (db/uuid)
              :name "Lean Pixel"}
        group (db/create-group! data)
        user-id (db/uuid)
        user-2-id (db/uuid)]
    (testing "can create a group"
      (is (= group (assoc data :extensions () :admins #{} :intro nil))))
    (testing "can add a user to the group"
      (let [user (db/create-user! {:id user-id
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})]
        (is (= #{} (db/get-groups-for-user (user :id))))
        (is (= #{} (db/get-users-in-group (group :id))))
        (db/user-add-to-group! (user :id) (group :id))
        (is (= #{(assoc data :extensions () :admins #{} :intro nil)}
               (db/get-groups-for-user (user :id))))
        (is (= #{(dissoc user :group-ids)}
               (set (map (fn [u] (dissoc user :group-ids))
                    (db/get-users-in-group (group :id))))))))
    (testing "groups have no admins by default"
      (is (empty? (:admins (db/get-group (group :id))))))
    (testing "Can add admin"
      (db/user-make-group-admin! user-id (group :id))
      (is (= #{user-id} (:admins (db/get-group (group :id)))))
      (testing "and another admin"
        (db/create-user! {:id user-2-id
                          :email "bar@baz.com"
                          :password "foobar"
                          :avatar "http://www.foobar.com/1.jpg"})
        (db/user-make-group-admin! user-2-id (group :id))
        (is (= #{user-id user-2-id} (:admins (db/get-group (group :id)))))))
    (testing "multiple groups, admin statuses"
      (let [group-2 (db/create-group! {:id (db/uuid)
                                       :name "another group"})
            group-3 (db/create-group! {:id (db/uuid)
                                       :name "third group"})]

        (db/user-add-to-group! user-id (group-2 :id))
        (db/user-make-group-admin! user-2-id (group-2 :id))

        (db/user-make-group-admin! user-id (group-3 :id))
        (db/user-add-to-group! user-2-id (group-3 :id))

        (is (db/user-in-group? user-id (group-2 :id)))
        (is (db/user-in-group? user-id (group-3 :id)))

        (is (db/user-in-group? user-2-id (group-2 :id)))
        (is (db/user-in-group? user-2-id (group-3 :id)))

        (is (= #{(group :id) (group-2 :id) (group-3 :id)}
               (into #{} (map :id) (db/get-groups-for-user user-id))
               (into #{} (map :id) (db/get-groups-for-user user-2-id)))
            "Both users are in all the groups")

        (is (= #{user-2-id} (:admins (db/get-group (group-2 :id)))))
        (is (= #{user-id} (:admins (db/get-group (group-3 :id)))))

        (is (db/user-is-group-admin? user-id (group :id)))
        (is (not (db/user-is-group-admin? user-id (group-2 :id))))
        (is (db/user-is-group-admin? user-id (group-3 :id)))

        (is (db/user-is-group-admin? user-2-id (group :id)))
        (is (db/user-is-group-admin? user-2-id (group-2 :id)))
        (is (not (db/user-is-group-admin? user-2-id (group-3 :id))))

        ))))

(deftest fetch-messages-test
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        thread-1-id (db/uuid)
        message-1-data {:id (db/uuid)
                        :user-id (user-1 :id)
                        :thread-id thread-1-id
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
      (is (= (set messages) #{message-1 message-2})))
    (testing "Can retrieve threads"
      (is (= (db/get-thread thread-1-id)
             {:id thread-1-id
              :messages (map #(dissoc % :thread-id)
                             [message-1-data message-2-data])
              :tag-ids () :mentioned-ids ()}))
      (let [thread-2-id (db/uuid)
            message-3-data {:id (db/uuid)
                            :user-id (user-1 :id)
                            :thread-id thread-2-id
                            :created-at (java.util.Date.)
                            :content "Blurrp"}
            message-3 (db/create-message! message-3-data)]
        (is (= (db/get-threads [thread-1-id thread-2-id])
               [{:id thread-1-id
                 :messages (map #(dissoc % :thread-id)
                                [message-1-data message-2-data])
                 :tag-ids () :mentioned-ids ()}
                {:id thread-2-id
                 :messages (map #(dissoc % :thread-id)
                                [message-3-data])
                 :tag-ids () :mentioned-ids ()}]))))))

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

    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-subscribe-to-tag! (user-2 :id) (tag-1 :id))
    (db/user-add-to-group! (user-3 :id) (group-2 :id))
    (db/user-subscribe-to-group-tags! (user-3 :id) (group-2 :id))
    (db/create-message! {:thread-id thread-1-id :id (db/uuid) :content "zzz"
                         :user-id (user-1 :id) :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-1 :id)] })
    (db/create-message! {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                         :user-id (user-2 :id) :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-2 :id)]})


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

#_(deftest tagging-thread-with-disjoint-groups
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
                         :user-id (user-1 :id) :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-1 :id) (tag-2 :id)]})

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

(deftest extension-permissions
  (let [group-1 (db/create-group! {:id (db/uuid) :name "g1"})
        group-2 (db/create-group! {:id (db/uuid) :name "g2"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-2 :id)})
        user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        thread-1-id (db/uuid)
        thread-2-id (db/uuid)
        ext-1 (db/create-extension! {:id (db/uuid)
                                     :type :asana
                                     :user-id (user-1 :id)
                                     :group-id (group-1 :id)
                                     :config {:type :asana :tag-id (tag-1 :id)}})
        ext-2 (db/create-extension! {:id (db/uuid)
                                     :type :asana
                                     :user-id (user-1 :id)
                                     :group-id (group-2 :id)
                                     :config {:type :asana :tag-id (tag-2 :id)}})]

    (testing "extensions can only see threads in the group they are in"
      (is (not (db/thread-visible-to-extension? thread-1-id (ext-1 :id))))

      (db/create-message! {:thread-id thread-1-id :id (db/uuid) :content "zzz"
                           :user-id (user-1 :id) :created-at (java.util.Date.)
                           :mentioned-tag-ids [(tag-1 :id)]})
      (db/create-message! {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                           :user-id (user-1 :id) :created-at (java.util.Date.)
                           :mentioned-tag-ids [(tag-2 :id)]})

      (is (db/thread-visible-to-extension? thread-1-id (ext-1 :id)))
      (is (not (db/thread-visible-to-extension? thread-2-id (ext-1 :id))))
      (is (db/thread-visible-to-extension? thread-2-id (ext-2 :id)))

      )))

(deftest user-preferences
  (testing "Can set and retrieve preferences"
    (let [u (db/create-user! {:id (db/uuid)
                              :email "foo@bar.com"
                              :password "foobar"
                              :avatar ""})]
      (is (empty? (db/get-user-preferences (:id u))))
      (db/user-set-preference! (:id u) :email-frequency :weekly)
      (is (= {:email-frequency :weekly}
             (db/get-user-preferences (:id u))))
      (testing "can search by preferences"
        (let [u1 (:id (db/create-user! {:id (db/uuid)
                                        :email "foo@baz.com"
                                        :password "foobar"
                                        :avatar ""
                                        :nickname "zzz"}))
              u2 (:id (db/create-user! {:id (db/uuid)
                                        :email "bar@bar.com"
                                        :password "foobar"
                                        :avatar ""}))
              u3 (:id (db/create-user! {:id (db/uuid)
                                        :email "baz@bar.com"
                                        :password "foobar"
                                        :avatar ""}))]
          (db/user-set-preference! u1 :email-frequency :daily)
          (db/user-set-preference! u1 :favourite-color "blue")
          (db/user-set-preference! u2 :email-frequency :weekly)
          (db/user-set-preference! u2 :favourite-color "blue")
          (is (= #{u2 (u :id)} (set (db/user-search-preferences :email-frequency :weekly))))
          (is (= [u1] (db/user-search-preferences :email-frequency :daily)))
          (is (= #{u1 u2} (set (db/user-search-preferences :favourite-color "blue")))))))))
