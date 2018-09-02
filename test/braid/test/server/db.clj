(ns braid.test.server.db
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.test :refer :all]
    [braid.core.server.db :as db]
    [braid.core.server.db.bot :as bot]
    [braid.core.server.db.group :as group]
    [braid.core.server.db.invitation :as invitation]
    [braid.core.server.db.message :as message]
    [braid.core.server.db.tag :as tag]
    [braid.core.server.db.thread :as thread]
    [braid.core.server.db.user :as user]
    [braid.core.common.schema :as schema]
    [braid.core.common.util :as util]
    [braid.search.server :as search]
    [braid.test.fixtures.db :refer [drop-db]]
    [braid.test.server.test-utils :refer [fetch-messages]]))

(use-fixtures :each drop-db)

(deftest fetch-users
  (let [user-1-data {:id (db/uuid)
                     :email "foo@bar.com"}
        user-2-data  {:id (db/uuid)
                      :email "bar@baz.com"}
        [user-1 user-2] (db/run-txns!
                          (concat (user/create-user-txn user-1-data)
                                  (user/create-user-txn user-2-data)))
        [group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :name "aoeu" :slug "aoeu"}))
        _ (db/run-txns!
            (concat
              (group/user-add-to-group-txn (user-1 :id) (group :id))
              (group/user-add-to-group-txn (user-2 :id) (group :id))))
        users (user/users-for-user (user-1 :id))]
    (testing "returns all users"
      (is (= (set (map :id users))
             (set (map :id [user-1 user-2])))))
    (testing "get user by email"
      (is (= (:id user-1)
             (:id (user/user-with-email (user-1-data :email)))))
      (is (nil? (user/user-with-email "zzzzz@zzzzzz.ru"))))))

(deftest only-see-users-in-group
  (let [[group-1 group-2 user-1 user-2 user-3]
        (db/run-txns!
          (concat
            (group/create-group-txn {:id (db/uuid) :slug "g1" :name "g1"})
            (group/create-group-txn {:id (db/uuid) :slug "g2" :name "g2"})
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
    (is (= (set (map :id [user-1 user-2]))
           (set (map :id (user/users-for-user (user-1 :id))))))
    (is (= (set (map :id [user-1 user-2 user-3]))
           (set (map :id (user/users-for-user (user-2 :id))))))
    (is (= (set (map :id [user-2 user-3]))
           (set (map :id (user/users-for-user (user-3 :id))))))
    (is (user/user-visible-to-user? (user-1 :id) (user-2 :id)))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-3 :id))))
    (is (not (user/user-visible-to-user? (user-3 :id) (user-1 :id))))
    (is (user/user-visible-to-user? (user-2 :id) (user-3 :id)))
    (is (user/user-visible-to-user? (user-3 :id) (user-2 :id)))
    (db/run-txns! (group/user-leave-group-txn (user-1 :id) (group-1 :id)))
    (is (not (group/user-in-group? (user-1 :id) (group-1 :id))))
    (is (not (user/user-visible-to-user? (user-1 :id) (user-2 :id))))))

(deftest authenticate-user
  (let [user {:id (db/uuid)
              :email "fOo@bar.com"}
        password "asdf"]

    (db/run-txns! (user/create-user-txn user))
    (db/run-txns! (user/set-user-password-txn (user :id) password))

    (testing "returns user-id when email+password matches"
      (is (= (:id user)
             (user/authenticate-user (user :email) password))))

    (testing "email is case-insensitive"
      (is (= (:id user)
             (user/authenticate-user "Foo@bar.com" password)
             (user/authenticate-user "foo@bar.com" password)
             (user/authenticate-user "FOO@BAR.COM" password))))

    (testing "returns nil when email+password wrong"
      (is (nil? (user/authenticate-user (user :email) "zzz"))))))

(deftest fetch-messages-test
  (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid) :slug "group" :name "group"}))
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
        message-2-data {:id (db/uuid)
                        :group-id (group :id)
                        :user-id (user-1 :id)
                        :thread-id (message-1-data :thread-id)
                        :created-at (java.util.Date.)
                        :content "Hello?"}
        [message-1] (db/run-txns! (message/create-message-txn message-1-data))
        [message-2] (db/run-txns! (message/create-message-txn message-2-data))
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
            [message-3] (db/run-txns! (message/create-message-txn message-3-data))]
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
  (let [[group user-1] (db/run-txns!
                         (concat
                           (group/create-group-txn {:id (db/uuid) :slug "group" :name "group"})
                           (user/create-user-txn {:id (db/uuid)
                                                  :email "foo@bar.com"
                                                  :password "foobar"
                                                  :avatar ""})))
        [message-1 message-1-b message-2]
        (db/run-txns!
          (concat
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-1 :id)
                                         :thread-id (db/uuid)
                                         :created-at (java.util.Date.)
                                         :content "Hello?"})))]
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
        (db/run-txns!
          (message/create-message-txn {:id (db/uuid)
                                       :group-id (group :id)
                                       :user-id (user-2 :id)
                                       :thread-id (message-1 :thread-id)
                                       :created-at (java.util.Date.)
                                       :content "wake up"}))
        (is (contains?
              (set (map :id (thread/open-threads-for-user (user-1 :id))))
              (message-1 :thread-id)))
        (testing "unless the user has unsubscribed from the thread"
          (db/run-txns!
            (thread/user-unsubscribe-from-thread-txn (user-1 :id)
                                                     (message-2 :thread-id)))
          (db/run-txns!
            (message/create-message-txn {:id (db/uuid)
                                         :group-id (group :id)
                                         :user-id (user-2 :id)
                                         :thread-id (message-2 :thread-id)
                                         :created-at (java.util.Date.)
                                         :content "wake up"}))
          (is (not (contains?
                     (set (map :id (thread/open-threads-for-user (user-1 :id))))
                     (message-2 :thread-id))))
          (testing "but they get re-subscribed if they get mentioned/another tag is added"
            (db/run-txns!
              (message/create-message-txn {:id (db/uuid)
                                           :group-id (group :id)
                                           :user-id (user-2 :id)
                                           :thread-id (message-2 :thread-id)
                                           :created-at (java.util.Date.)
                                           :content "wake up"
                                           :mentioned-user-ids [(user-1 :id)]}))
            (is (contains?
                  (set (map :id (thread/open-threads-for-user (user-1 :id))))
                  (message-2 :thread-id)))))))))

(deftest user-thread-visibility
  (let [[user-1 user-2 user-3 group-1 group-2]
        (db/run-txns!
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
                                         :avatar ""})

                  (group/create-group-txn {:id (db/uuid)
                                           :slug "leanpixel"
                                           :name "Lean Pixel"})
                  (group/create-group-txn {:id (db/uuid)
                                           :slug "penyopal"
                                           :name "Penyo Pal"})))
        [tag-1 tag-2] (db/run-txns!
                        (concat
                          (tag/create-tag-txn {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
                          (tag/create-tag-txn {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})))

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
    (db/run-txns!
      (concat
        (message/create-message-txn {:thread-id thread-1-id :id (db/uuid) :content "zzz"
                                     :user-id (user-1 :id) :created-at (java.util.Date.)
                                     :mentioned-tag-ids [(tag-1 :id)] :group-id (group-1 :id)})
        (message/create-message-txn {:thread-id thread-2-id :id (db/uuid) :content "zzz"
                                     :user-id (user-2 :id) :created-at (java.util.Date.)
                                     :mentioned-tag-ids [(tag-2 :id)] :group-id (group-2 :id)})))


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
  (let [[user-1 user-2 group] (db/run-txns!
                                (concat
                                  (user/create-user-txn {:id (db/uuid)
                                                         :email "foo@bar.com"
                                                         :password "foobar"
                                                         :avatar ""})
                                  (user/create-user-txn {:id (db/uuid)
                                                         :email "bar@baz.com"
                                                         :password "foobar"
                                                         :avatar ""})
                                  (group/create-group-txn {:name "group 1"
                                                           :slug "group1"
                                                           :id (db/uuid)})))]
    (db/run-txns! (group/user-add-to-group-txn (user-1 :id) (group :id)))
    (is (empty? (invitation/invites-for-user (user-1 :id))))
    (is (empty? (invitation/invites-for-user (user-2 :id))))
    (let [invite-id (db/uuid)
          [invite] (db/run-txns!
                   (invitation/create-invitation-txn
                     {:id invite-id
                      :inviter-id (user-1 :id)
                      :invitee-email "bar@baz.com"
                      :group-id (group :id)}))]
      (is (= invite (invitation/invite-by-id invite-id)))
      (is (seq (invitation/invites-for-user (user-2 :id))))
      (db/run-txns! (invitation/retract-invitation-txn invite-id))
      (is (empty? (invitation/invites-for-user (user-2 :id)))))))

(deftest user-leaving-group
  (let [[user-1 group] (db/run-txns!
                         (concat
                           (user/create-user-txn {:id (db/uuid)
                                                  :email "foo@bar.com"
                                                  :password "foobar"
                                                  :avatar ""})
                           (group/create-group-txn {:id (db/uuid)
                                                    :slug "group1"
                                                    :name "group 1"})))
        thread-id (db/uuid)]
    (db/run-txns!
      (message/create-message-txn {:id (db/uuid) :thread-id thread-id
                                   :group-id (group :id) :user-id (user-1 :id)
                                   :created-at (java.util.Date.)
                                   :content "foobar"
                                   :mentioned-user-ids [(user-1 :id)]
                                   :mentioned-tag-ids []}))
    (testing "user leaving group removes mentions of that user"
      (is (= #{(user-1 :id)}
             (:mentioned-ids (thread/thread-by-id thread-id))))
      (db/run-txns! (group/user-leave-group-txn (user-1 :id) (group :id)))
      (is (empty? (:mentioned-ids (thread/thread-by-id thread-id)))))))

(deftest adding-user-to-group-subscribes-tags
  (let [[user group] (db/run-txns!
                       (concat
                         (user/create-user-txn {:id (db/uuid)
                                                :email "foo@bar.com"
                                                :password "foobar"
                                                :avatar ""})
                         (group/create-group-txn {:name "group" :slug "group" :id (db/uuid)})))
        group-tags (db/run-txns!
                     (mapcat
                       tag/create-tag-txn
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
  (let [[g1 g2 g3] (db/run-txns!
                     (concat
                       (group/create-group-txn {:name "group 1" :slug "group1" :id (db/uuid)})
                       (group/create-group-txn {:name "group 2" :slug "group2" :id (db/uuid)})
                       (group/create-group-txn {:name "group 3" :slug "group3" :id (db/uuid)})))

        [b1 b2 b3] (db/run-txns!
                     (concat
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot1"
                                            :avatar ""
                                            :webhook-url ""
                                            :event-webhook-url ""
                                            :group-id (g1 :id)})
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot2"
                                            :avatar ""
                                            :webhook-url ""
                                            :event-webhook-url ""
                                            :group-id (g1 :id)})
                       (bot/create-bot-txn {:id (db/uuid)
                                            :name "bot3"
                                            :avatar ""
                                            :webhook-url ""
                                            :event-webhook-url ""
                                            :group-id (g2 :id)})))
        bot->display (fn [b] (-> b
                                 (select-keys [:id :name :avatar :user-id])
                                 (rename-keys {:name :nickname})))]

    (is (util/valid? schema/Bot b1))
    (is (util/valid? schema/Bot b2))
    (is (util/valid? schema/Bot b3))
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

(deftest update-thread-open-at
  (let [[{user-id :id} {group-id :id}]
        (db/run-txns!
          (concat
            (user/create-user-txn {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
            (group/create-group-txn {:id (db/uuid)
                                     :slug "group-1"
                                     :name "group 1"})))
        thread-id (db/uuid)]
    (db/run-txns!
      (message/create-message-txn {:id (db/uuid)
                                   :thread-id thread-id
                                   :group-id group-id
                                   :user-id user-id
                                   :created-at (java.util.Date.)
                                   :content "foobar"
                                   :mentioned-user-ids [user-id]
                                   :mentioned-tag-ids []}))
    (testing  "can update when a thread was last opened by user"
      (let [thread (thread/thread-by-id thread-id)
            old-open (:last-open-at (thread/thread-add-last-open-at thread user-id))
            _ (thread/update-thread-last-open! thread-id user-id)
            new-open (:last-open-at (thread/thread-add-last-open-at thread user-id))]
        (is (< old-open new-open))))))

(deftest bot-thread-watching
  (let [[{user-id :id} {group-1-id :id} {group-2-id :id}]
        (db/run-txns!
          (concat
            (user/create-user-txn {:id (db/uuid)
                                   :email "foo@bar.com"
                                   :password "foobar"
                                   :avatar ""})
            (group/create-group-txn {:id (db/uuid)
                                     :slug "group 1"
                                     :name "group 1"})
            (group/create-group-txn {:id (db/uuid)
                                     :slug "group 2"
                                     :name "group 2"})))
        [{bot-id :id :as bot}] (db/run-txns!
                                 (bot/create-bot-txn {:id (db/uuid)
                                                      :name "testbot"
                                                      :avatar ""
                                                      :webhook-url ""
                                                      :group-id group-1-id}))
        thread-1-id (db/uuid)
        thread-2-id (db/uuid)]
    (db/run-txns!
      (concat
        (group/user-join-group-txn user-id group-1-id)
        (group/user-join-group-txn user-id group-2-id)))
    (db/run-txns!
      (concat
        (message/create-message-txn {:id (db/uuid)
                                     :thread-id thread-1-id
                                     :group-id group-1-id
                                     :user-id user-id
                                     :created-at (java.util.Date.)
                                     :content "foobar"
                                     :mentioned-user-ids [user-id]
                                     :mentioned-tag-ids []})
        (message/create-message-txn {:id (db/uuid)
                                     :thread-id thread-2-id
                                     :group-id group-2-id
                                     :user-id user-id
                                     :created-at (java.util.Date.)
                                     :content "foobar"
                                     :mentioned-user-ids [user-id]
                                     :mentioned-tag-ids []})))
    (testing "Bots can only watch threads in their group"
      (db/run-txns! (bot/bot-watch-thread-txn bot-id thread-1-id))
      (is (thrown? clojure.lang.ExceptionInfo
                   (db/run-txns! (bot/bot-watch-thread-txn bot-id thread-2-id))))
      (is (= #{bot} (bot/bots-watching-thread thread-1-id)))
      (is (empty? (bot/bots-watching-thread thread-2-id))))))
