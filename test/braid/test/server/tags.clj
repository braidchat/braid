(ns braid.test.server.tags
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [braid.server.conf :as conf]
            [braid.server.db :as db]))


(use-fixtures :each
              (fn [t]
                (-> (mount/only #{#'conf/config #'db/conn})
                    (mount/swap {#'conf/config
                                 {:db-url "datomic:mem://chat-test"}})
                    (mount/start))
                (t)
                (datomic.api/delete-database (conf/config :db-url))
                (mount/stop)))


(deftest tags
  (testing "can create tag"
    (let [group (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
          tag-data {:id (db/uuid)
                    :name "acme"
                    :group-id (group :id)}]
      (testing "create-tag!"
        (let [tag (db/create-tag! tag-data)]
          (testing "returns tag"
            (is (= tag (assoc tag-data
                         :description nil
                         :threads-count 0
                         :subscribers-count 0))))))
      (testing "set tag description"
        (db/tag-set-description! (:id tag-data) "Some tag with stuff")
        (is (= (first (db/group-tags (:id group)))
               (assoc tag-data
                 :description "Some tag with stuff"
                 :threads-count 0
                 :subscribers-count 0)))
        ))))

(deftest user-can-subscribe-to-tags
  (let [user (db/create-user! {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""})
        group (db/create-group! {:id (db/uuid)
                                 :name "Lean Pixel"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group :id)})]
    (db/user-add-to-group! (user :id) (group :id))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/user-subscribe-to-tag! (user :id) (tag-1 :id))
        (db/user-subscribe-to-tag! (user :id) (tag-2 :id)))
      (testing "get-user-subscribed-tags"
        (let [tags (db/subscribed-tag-ids-for-user (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id) (tag-2 :id)}))))))
    (testing "user can unsubscribe from tags"
      (testing "user-unsubscribe-from-tag!"
        (db/user-unsubscribe-from-tag! (user :id) (tag-1 :id))
        (db/user-unsubscribe-from-tag! (user :id) (tag-2 :id)))
      (testing "is unsubscribed"
        (let [tags (db/subscribed-tag-ids-for-user (user :id))]
          (is (= (set tags) #{})))))))

(deftest user-can-only-see-tags-in-group
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
        tag-3 (db/create-tag! {:id (db/uuid) :name "acme3" :group-id (group-2 :id)})]
    (db/user-add-to-group! (user-1 :id) (group-1 :id))
    (db/user-add-to-group! (user-2 :id) (group-1 :id))
    (db/user-add-to-group! (user-2 :id) (group-2 :id))
    (db/user-add-to-group! (user-3 :id) (group-2 :id))
    (testing "user can only see tags in their group(s)"
      (is (= #{tag-1} (db/tags-for-user (user-1 :id))))
      (is (= #{tag-1 tag-2 tag-3} (db/tags-for-user (user-2 :id))))
      (is (= #{tag-2 tag-3} (db/tags-for-user (user-3 :id)))))))

(deftest user-can-only-subscribe-to-tags-in-group
  (let [user (db/create-user! {:id (db/uuid)
                               :email "foo@bar.com"
                               :password "foobar"
                               :avatar ""})
        group-1 (db/create-group! {:id (db/uuid)
                                   :name "Lean Pixel"})
        group-2 (db/create-group! {:id (db/uuid)
                                   :name "Penyo Pal"})
        tag-1 (db/create-tag! {:id (db/uuid) :name "acme1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "acme2" :group-id (group-2 :id)})]
    (db/user-add-to-group! (user :id) (group-1 :id))
    (testing "user can subscribe to tags"
      (testing "user-subscribe-to-tag!"
        (db/user-subscribe-to-tag! (user :id) (tag-1 :id))
        (db/user-subscribe-to-tag! (user :id) (tag-2 :id)))
      (testing "get-user-subscribed-tags"
        (let [tags (db/subscribed-tag-ids-for-user (user :id))]
          (testing "returns subscribed tags"
            (is (= (set tags) #{(tag-1 :id)}))))))))
