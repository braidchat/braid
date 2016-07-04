(ns braid.test.server.db
  (:require [clojure.test :refer :all]
            [clojure.set :refer [rename-keys]]
            [mount.core :as mount]
            [braid.test.server.test-utils :refer [fetch-messages]]
            [braid.server.conf :as conf]
            [braid.server.db :as db :refer [conn]]
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
      (db/set-nickname! (user :id) "ol' fooy")
      (is (db/nickname-taken? "ol' fooy"))
      (is (= (-> (db/user-with-email "foo@bar.com")
                 (dissoc :group-ids))
             (-> data
                 (dissoc :password :email)
                 (assoc :nickname "ol' fooy")))))

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
        (is (thrown? java.util.concurrent.ExecutionException
                     @(db/set-nickname! (other :id)  "ol' fooy")))))))

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
        users (db/users-for-user (user-1 :id))]
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
      (is (= group (assoc data :admins #{} :intro nil :avatar nil
                     :public? false :bots #{}))))
    (testing "can set group intro"
      (db/group-set! (group :id) :intro "the intro")
      (is (= (db/group-by-id (group :id))
             (assoc data :admins #{} :intro "the intro" :avatar nil
               :public? false :bots #{}))))
    (testing "can add a user to the group"
      (let [user (db/create-user! {:id user-id
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar "http://www.foobar.com/1.jpg"})]
        (is (= #{} (db/user-groups (user :id))))
        (is (= #{} (db/group-users (group :id))))
        (db/user-add-to-group! (user :id) (group :id))
        (is (= #{(assoc data :admins #{} :intro "the intro" :avatar nil
                   :public? false :bots #{})}
               (db/user-groups (user :id))))
        (is (= #{(dissoc user :group-ids)}
               (set (map (fn [u] (dissoc user :group-ids))
                    (db/group-users (group :id))))))))
    (testing "groups have no admins by default"
      (is (empty? (:admins (db/group-by-id (group :id))))))
    (testing "Can add admin"
      (db/user-make-group-admin! user-id (group :id))
      (is (= #{user-id} (:admins (db/group-by-id (group :id)))))
      (testing "and another admin"
        (db/create-user! {:id user-2-id
                          :email "bar@baz.com"
                          :password "foobar"
                          :avatar "http://www.foobar.com/1.jpg"})
        (db/user-make-group-admin! user-2-id (group :id))
        (is (= #{user-id user-2-id} (:admins (db/group-by-id (group :id)))))))
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
               (into #{} (map :id) (db/user-groups user-id))
               (into #{} (map :id) (db/user-groups user-2-id)))
            "Both users are in all the groups")

        (is (= #{user-2-id} (:admins (db/group-by-id (group-2 :id)))))
        (is (= #{user-id} (:admins (db/group-by-id (group-3 :id)))))

        (is (db/user-is-group-admin? user-id (group :id)))
        (is (not (db/user-is-group-admin? user-id (group-2 :id))))
        (is (db/user-is-group-admin? user-id (group-3 :id)))

        (is (db/user-is-group-admin? user-2-id (group :id)))
        (is (db/user-is-group-admin? user-2-id (group-2 :id)))
        (is (not (db/user-is-group-admin? user-2-id (group-3 :id))))

        ))))

(deftest fetch-messages-test
  (let [group (db/create-group! {:id (db/uuid) :name "group"})
        user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar "http://www.foobar.com/1.jpg"})
        thread-1-id (db/uuid)
        message-1-data {:id (db/uuid)
                        :group-id (group :id)
                        :user-id (user-1 :id)
                        :thread-id thread-1-id
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-1 (db/create-message! message-1-data)
        message-2-data {:id (db/uuid)
                        :group-id (group :id)
                        :user-id (user-1 :id)
                        :thread-id (message-1 :thread-id)
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        message-2 (db/create-message! message-2-data)
        messages (fetch-messages)]
    (testing "fetch-messages returns all messages"
      (is (= (set messages) #{message-1 message-2})))
    (testing "Can retrieve threads"
      (is (= (db/thread-by-id thread-1-id)
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
            message-3 (db/create-message! message-3-data)]
        (is (= (db/threads-by-id [thread-1-id thread-2-id])
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
  (let [group (db/create-group! {:id (db/uuid) :name "group"})
        user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        message-1 (db/create-message! {:id (db/uuid)
                                       :group-id (group :id)
                                       :user-id (user-1 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})
        message-1-b (db/create-message! {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})
        message-2 (db/create-message! {:id (db/uuid)
                                       :group-id (group :id)
                                       :user-id (user-1 :id)
                                       :thread-id (db/uuid)
                                       :created-at (java.util.Date.)
                                       :content "Hello?"})]
    (testing "thread 1 is open"
      (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id))))
                     (message-1 :thread-id))))
    (testing "thread 2 is open"
      (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-2 :thread-id))))
    (testing "user can hide thread"
      (db/user-hide-thread! (user-1 :id) (message-1 :thread-id))
      (is (not (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-1 :thread-id)))))
    (testing "user can hide thread"
      (db/user-hide-thread! (user-1 :id) (message-2 :thread-id))
      (is (not (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-2 :thread-id)))))
    (testing "thread is re-opened when it gets another message"
      (let [user-2 (db/create-user! {:id (db/uuid) :email "bar@baz.com"
                                     :password "foobar" :avatar ""})]
        (db/create-message! {:id (db/uuid)
                             :group-id (group :id)
                             :user-id (user-2 :id)
                             :thread-id (message-1 :thread-id)
                             :created-at (java.util.Date.)
                             :content "wake up"})
        (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-1 :thread-id)))
        (testing "unless the user has unsubscribed from the thread"
          (db/user-unsubscribe-from-thread! (user-1 :id) (message-2 :thread-id))
          (db/create-message! {:id (db/uuid)
                               :group-id (group :id)
                               :user-id (user-2 :id)
                               :thread-id (message-2 :thread-id)
                               :created-at (java.util.Date.)
                               :content "wake up"})
          (is (not (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-2 :thread-id))))
          (testing "but they get re-subscribed if they get mentioned/another tag is added"
            (db/create-message! {:id (db/uuid)
                                 :group-id (group :id)
                                 :user-id (user-2 :id)
                                 :thread-id (message-2 :thread-id)
                                 :created-at (java.util.Date.)
                                 :content "wake up"
                                 :mentioned-user-ids [(user-1 :id)]})
            (is (contains? (set (map :id (db/open-threads-for-user (user-1 :id)))) (message-2 :thread-id)))))))))

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
                         :mentioned-tag-ids [(tag-1 :id)] :group-id (group-1 :id)})
    (db/create-message! {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                         :user-id (user-2 :id) :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-2 :id)] :group-id (group-2 :id)})


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
      (is (db/user-can-see-thread? (user-3 :id) thread-2-id))
      (testing "but they can't after leaving"
        (db/user-leave-group! (user-3 :id) (group-2 :id))
        (is (not (db/user-can-see-thread? (user-3 :id) thread-2-id)))))
    (testing "user can leave one group and still see threads in the other"
      (db/user-leave-group! (user-2 :id) (group-1 :id))
      (is (not (db/user-can-see-thread? (user-2 :id) thread-1-id)))
      (is (db/user-can-see-thread? (user-2 :id) thread-2-id)))))

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
    (is (empty? (db/invites-for-user (user-1 :id))))
    (is (empty? (db/invites-for-user (user-2 :id))))
    (let [invite-id (db/uuid)
          invite (db/create-invitation! {:id invite-id
                                         :inviter-id (user-1 :id)
                                         :invitee-email "bar@baz.com"
                                         :group-id (group :id)})]
      (is (= invite (db/invite-by-id invite-id)))
      (is (seq (db/invites-for-user (user-2 :id))))
      (db/retract-invitation! invite-id)
      (is (empty? (db/invites-for-user (user-2 :id)))))))

(deftest user-leaving-group
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        group (db/create-group! {:id (db/uuid) :name "group 1"})
        thread-id (db/uuid)]
    (db/create-message! {:id (db/uuid) :thread-id thread-id
                         :group-id (group :id) :user-id (user-1 :id)
                         :created-at (java.util.Date.)
                         :content "foobar"
                         :mentioned-user-ids [(user-1 :id)]
                         :mentioned-tag-ids []})
    (testing "user leaving group removes mentions of that user"
      (is (= #{(user-1 :id)}
             (:mentioned-ids (db/thread-by-id thread-id))))
      (db/user-leave-group! (user-1 :id) (group :id))
      (is (empty? (:mentioned-ids (db/thread-by-id thread-id)))))))

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
      (is (= group (db/group-by-id (group :id))))
      (is (= (set group-tags)
             (set (db/group-tags (group :id))))))
    (db/user-add-to-group! (user :id) (group :id))
    (db/user-subscribe-to-group-tags! (user :id) (group :id))
    (is (= (set (db/subscribed-tag-ids-for-user (user :id)))
           (db/tag-ids-for-user (user :id))
           (set (map :id group-tags))))
    (testing "can remove tags"
      (db/retract-tag! (:id (first group-tags)))
      (is (= 2 (count (db/group-tags (group :id))))))))

(deftest user-preferences
  (testing "Can set and retrieve preferences"
    (let [u (db/create-user! {:id (db/uuid)
                              :email "foo@bar.com"
                              :password "foobar"
                              :avatar ""})]
      (is (empty? (db/user-get-preferences (:id u))))
      (db/user-set-preference! (:id u) :email-frequency :weekly)
      (is (= {:email-frequency :weekly}
             (db/user-get-preferences (:id u))))
      (is (= :weekly (db/user-get-preference (:id u) :email-frequency)))
      (db/user-set-preference! (:id u) :email-frequency :daily)
      (is (= :daily (db/user-get-preference (:id u) :email-frequency)))
      (db/user-set-preference! (:id u) :email-frequency :weekly)
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

(deftest bots-test
  (let [g1 (db/create-group! {:name "group 1" :id (db/uuid)})
        g2 (db/create-group! {:name "group 2" :id (db/uuid)})
        g3 (db/create-group! {:name "group 3" :id (db/uuid)})

        b1 (db/create-bot! {:id (db/uuid)
                            :name "bot1"
                            :avatar ""
                            :webhook-url ""
                            :group-id (g1 :id)})
        b2 (db/create-bot! {:id (db/uuid)
                            :name "bot2"
                            :avatar ""
                            :webhook-url ""
                            :group-id (g1 :id)})
        b3 (db/create-bot! {:id (db/uuid)
                            :name "bot3"
                            :avatar ""
                            :webhook-url ""
                            :group-id (g2 :id)})
        bot->display (fn [b] (-> b
                                 (select-keys [:id :name :avatar :user-id])
                                 (rename-keys {:name :nickname})))]

    (is (schema/check-bot! b1))
    (is (schema/check-bot! b2))
    (is (schema/check-bot! b3))
    (testing "can create bots & retrieve by group"
      (is (= #{b1 b2} (db/bots-in-group (g1 :id))))
      (is (= (into #{}  (map bot->display) [b1 b2]) (:bots (db/group-by-id (g1 :id)))))
      (is (= #{b3} (db/bots-in-group (g2 :id))))
      (is (= #{} (db/bots-in-group (g3 :id))))
      (is (= b1 (db/bot-by-name-in-group "bot1" (g1 :id))))
      (is (nil? (db/bot-by-name-in-group "bot1" (g3 :id)))))
    (testing "can check bot auth"
      (is (db/bot-auth? (b1 :id) (b1 :token)))
      (is (db/bot-auth? (b2 :id) (b2 :token)))
      (is (not (db/bot-auth? (b2 :id) "Foo")))
      (is (not (db/bot-auth? (java.util.UUID/randomUUID) (b2 :token)))))))
