(ns braid.test.server.notify-rules-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]
    [braid.core.common.schema :refer [rules-valid? check-rules!]]
    [braid.core.server.db :as db]
    [braid.core.server.db.group :as group]
    [braid.core.server.db.message :as message]
    [braid.core.server.db.tag :as tag]
    [braid.core.server.db.user :as user]
    [braid.core.server.notify-rules :as rules]
    [braid.test.fixtures.db :refer [drop-db]]))

(s/set-fn-validation! true)

(use-fixtures :each drop-db)

(deftest rules-schema
  (testing "schema can validate rules format"
    (is (rules-valid? []))
    (is (rules-valid? [[:any :any]]))
    (is (thrown? clojure.lang.ExceptionInfo (check-rules! [[:any :any] []])))
    (is (not (rules-valid? [[:any :any] []])))
    (is (rules-valid?
          [[:any #uuid "570fce10-9312-4550-9525-460c57fd9229"]
           [:mention :any]
           [:tag #uuid "56c2653c-a1fb-4043-97ca-3060ec95d852"]
           [:tag #uuid "570fce1a-116c-46d1-8924-5ac28f8656cb"]
           [:tag #uuid "57158d7e-8dd1-4834-9e6b-cb7985895621"]]))))

(deftest notify-rules-work
  (let [[g1 g2 g3] (db/run-txns!
                     (concat
                       (group/create-group-txn {:id (db/uuid)
                                                :slug "group1"
                                                :name "group1"})
                       (group/create-group-txn {:id (db/uuid)
                                                :slug "group2"
                                                :name "group2"})
                       (group/create-group-txn {:id (db/uuid)
                                                :slug "group3"
                                                :name "group3"})))

        [g1t1 g1t2 g2t1 g2t2]
        (db/run-txns!
          (concat
            (tag/create-tag-txn {:id (db/uuid) :name "group1tag1" :group-id (g1 :id)})
            (tag/create-tag-txn {:id (db/uuid) :name "group1tag2" :group-id (g1 :id)})

            (tag/create-tag-txn {:id (db/uuid) :name "group2tag1" :group-id (g2 :id)})
            (tag/create-tag-txn {:id (db/uuid) :name "group2tag2" :group-id (g2 :id)})))

        user-id (db/uuid)
        [_ sender] (db/run-txns!
                     (concat
                       (user/create-user-txn {:id user-id
                                              :email "foo@bar.com"
                                              :avatar "zz"
                                              :password "foobar"})
                       (user/create-user-txn {:id (db/uuid)
                                              :email "bar@bar.com"
                                              :avatar "zz"
                                              :password "foobar"})))

        msg (fn [] {:id (db/uuid)
                    :group-id (g1 :id)
                    :thread-id (db/uuid)
                    :user-id (sender :id)
                    :content ""
                    :created-at (java.util.Date.)
                    :mentioned-user-ids ()
                    :mentioned-tag-ids ()})]

    (is (rules/notify? (db/uuid) [[:any :any]] (msg)) ":any :any always gets notified")
    (let [new-msg (msg)]
      (db/run-txns! (message/create-message-txn new-msg))
      (is (not (rules/notify? (db/uuid) [[:any (:id g1)]] new-msg))))
    (let [new-msg (update (msg) :mentioned-tag-ids conj (:id g1t1))]
      (db/run-txns! (message/create-message-txn new-msg))
      (is (rules/notify? (db/uuid) [[:any (:id g1)]]
                         new-msg)))
    (let [m1 (-> (msg)
                 (update :mentioned-user-ids conj user-id))]
      (db/run-txns! (message/create-message-txn m1))
      (is (not (rules/notify? user-id [[:mention (:id g1)]] m1))))

    (let [m2 (-> (msg)
                 (update :mentioned-user-ids conj user-id))]
      (db/run-txns! (message/create-message-txn m2))
      (is (rules/notify? user-id [[:mention :any]] m2)))

    (let [m (-> (msg)
                (update :mentioned-user-ids conj user-id)
                (update :mentioned-tag-ids conj (:id g1t1)))]
      (db/run-txns! (message/create-message-txn m))
      (is (rules/notify? user-id [[:mention (:id g1)]] m))
      (is (rules/notify? user-id [[:tag (:id g1t1)]] m))
      (is (not (rules/notify? user-id [[:tag (:id g1t2)]] m))))

    (let [m1 (-> (msg)
                 (update :mentioned-tag-ids conj (:id g1t1)))
          m2 (-> (msg) (assoc :thread-id (m1 :thread-id)))]
      (db/run-txns! (message/create-message-txn m1))
      (db/run-txns! (message/create-message-txn m2))
      (is (rules/notify? (db/uuid) [[:any (:id g1)]] m2))
      (is (not (rules/notify? (db/uuid) [[:any (:id g2)]] m2))))))

