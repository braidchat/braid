(ns braid.test.common.notify-rules-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [chat.server.db :as db]
            [braid.common.notify-rules :as rules]))

(s/set-fn-validation! true)

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://chat-test"]
                  (db/init!)
                  (db/with-conn (t))
                  (datomic.api/delete-database db/*uri*))))

(deftest rules-schema
  (testing "schema can validate rules format"
    (is (rules/rules-valid? []))
    (is (rules/rules-valid? [[:any :any]]))
    (is (thrown? clojure.lang.ExceptionInfo
                 (rules/rules-valid? [[:any :any] []])))
    (is (rules/rules-valid?
          [[:any #uuid "570fce10-9312-4550-9525-460c57fd9229"]
           [:mention :any]
           [:tag #uuid "56c2653c-a1fb-4043-97ca-3060ec95d852"]
           [:tag #uuid "570fce1a-116c-46d1-8924-5ac28f8656cb"]
           [:tag #uuid "57158d7e-8dd1-4834-9e6b-cb7985895621"]]))))

(deftest notify-rules-work
  (let [g1 (db/create-group! {:id (db/uuid)
                              :name "group1"})
        g2 (db/create-group! {:id (db/uuid)
                              :name "group2"})
        g3 (db/create-group! {:id (db/uuid)
                              :name "group3"})

        g1t1 (db/create-tag! {:id (db/uuid) :name "group1tag1" :group-id (g1 :id)})
        g1t2 (db/create-tag! {:id (db/uuid) :name "group1tag2" :group-id (g1 :id)})

        g2t1 (db/create-tag! {:id (db/uuid) :name "group2tag1" :group-id (g2 :id)})
        g2t2 (db/create-tag! {:id (db/uuid) :name "group2tag2" :group-id (g2 :id)})

        thread {:id (db/uuid)
                :messages ()
                :tag-ids ()
                :mentioned-ids ()}]

    (is (rules/notify? (db/uuid) [[:any :any]] thread) ":any :any always gets notified")
    (is (not (rules/notify? (db/uuid) [[:any (:id g1)]] thread)))
    (is (rules/notify? (db/uuid) [[:any (:id g1)]]
                       (update thread :tag-ids conj (:id g1t1))))
    (let [user-id (db/uuid)]
      (is (not (rules/notify? user-id [[:mention (:id g1)]]
                              (-> thread
                                  (update :mentioned-ids conj user-id)))))
      (is (rules/notify? user-id [[:mention :any]]
                         (-> thread
                             (update :mentioned-ids conj user-id))))
      (is (rules/notify? user-id [[:mention (:id g1)]]
                         (-> thread
                             (update :mentioned-ids conj user-id)
                             (update :tag-ids conj (:id g1t1))))))))
