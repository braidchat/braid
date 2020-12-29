(ns braid.test.server.tags-test
  (:require
   [clojure.test :refer :all]
   [braid.core.server.db :as db]
   [braid.chat.db.group :as group]
   [braid.chat.db.tag :as tag]
   [braid.chat.db.user :as user]
   [braid.test.fixtures.db :refer [drop-db]]))

(use-fixtures :each drop-db)

(deftest tags
  (testing "can create tag"
    (let [[group] (db/run-txns! (group/create-group-txn {:id (db/uuid)
                                                         :slug "leanpixel"
                                                         :name "Lean Pixel"}))
          tag-data {:id (db/uuid)
                    :name "acme"
                    :group-id (group :id)}]
      (testing "create-tag!"
        (let [[tag] (db/run-txns! (tag/create-tag-txn tag-data))]
          (testing "returns tag"
            (is (= tag (assoc tag-data
                              :description nil
                              :threads-count 0
                              :subscribers-count 0))))))
      (testing "set tag description"
        (db/run-txns!
         (tag/tag-set-description-txn (:id tag-data) "Some tag with stuff"))
        (is (= (first (group/group-tags (:id group)))
               (assoc tag-data
                      :description "Some tag with stuff"
                      :threads-count 0
                      :subscribers-count 0)))))))

(deftest user-can-subscribe-to-tags
  (let [[user group] (db/run-txns!
                      (concat
                       (user/create-user-txn {:id (db/uuid)
                                              :email "foo@bar.com"
                                              :password "foobar"
                                              :avatar ""})
                       (group/create-group-txn {:id (db/uuid)
                                                :slug "leanpixel"
                                                :name "Lean Pixel"})))
        [tag-1 tag-2] (db/run-txns!
                       (concat
                        (tag/create-tag-txn {:id (db/uuid) :name "acme1" :group-id (group :id)})
                        (tag/create-tag-txn {:id (db/uuid) :name "acme2" :group-id (group :id)})))]
    (db/run-txns! (group/user-add-to-group-txn (user :id) (group :id)))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/run-txns!
         (concat
          (tag/user-subscribe-to-tag-txn (user :id) (tag-1 :id))
          (tag/user-subscribe-to-tag-txn (user :id) (tag-2 :id)))))
      (testing "get-user-subscribed-tags"
        (let [tags (tag/subscribed-tag-ids-for-user (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id) (tag-2 :id)}))))))
    (testing "user can unsubscribe from tags"
      (testing "user-unsubscribe-from-tag!"
        (db/run-txns!
         (concat
          (tag/user-unsubscribe-from-tag-txn (user :id) (tag-1 :id))
          (tag/user-unsubscribe-from-tag-txn (user :id) (tag-2 :id)))))
      (testing "is unsubscribed"
        (let [tags (tag/subscribed-tag-ids-for-user (user :id))]
          (is (= (set tags) #{})))))))

(deftest user-can-only-see-tags-in-group
  (let [[user-1 user-2 user-3 group-1 group-2]
        (db/run-txns!
         (concat
          (user/create-user-txn {:id (db/uuid)
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
        [tag-1 tag-2 tag-3]
        (db/run-txns!
         (concat
          (tag/create-tag-txn {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
          (tag/create-tag-txn {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})
          (tag/create-tag-txn {:id (db/uuid) :name "acme3" :group-id (group-2 :id)})))]
    (db/run-txns!
     (concat
      (group/user-add-to-group-txn (user-1 :id) (group-1 :id))
      (group/user-add-to-group-txn (user-2 :id) (group-1 :id))
      (group/user-add-to-group-txn (user-2 :id) (group-2 :id))
      (group/user-add-to-group-txn (user-3 :id) (group-2 :id))))
    (testing "user can only see tags in their group(s)"
      (is (= #{tag-1} (tag/tags-for-user (user-1 :id))))
      (is (= #{tag-1 tag-2 tag-3} (tag/tags-for-user (user-2 :id))))
      (is (= #{tag-2 tag-3} (tag/tags-for-user (user-3 :id)))))))

(deftest user-can-only-subscribe-to-tags-in-group
  (let [[user group-1 group-2] (db/run-txns!
                                (concat
                                 (user/create-user-txn {:id (db/uuid)
                                                        :email "foo@bar.com"
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
                        (tag/create-tag-txn {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})))]
    (db/run-txns! (group/user-add-to-group-txn (user :id) (group-1 :id)))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/run-txns!
         (concat
          (tag/user-subscribe-to-tag-txn (user :id) (tag-1 :id))
          (tag/user-subscribe-to-tag-txn (user :id) (tag-2 :id)))))
      (testing "get-user-subscribed-tags"
        (let [tags (tag/subscribed-tag-ids-for-user (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id)}))))))))
