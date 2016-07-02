(ns braid.test.server.search
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [braid.server.conf :as conf]
            [braid.server.db :as db]
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


(deftest query-parsing
  (testing "can properly parse queries"
    (is (= (search/parse-query "#baz")
           {:text ""
            :tags ["baz"]}))
    (is (= (search/parse-query "#baz #quux")
           {:text ""
            :tags ["baz" "quux"]}))
    (is (= (search/parse-query "baz")
           {:text "baz"
            :tags []}))
    (is (= (search/parse-query "foo bar #baz quux")
           {:text "foo bar quux"
            :tags ["baz"]}))
    (is (= (search/parse-query "#foo bar #baz quux")
           {:text "bar quux"
            :tags ["foo" "baz"]}))))

(deftest searching-threads
  (let [user-1 (db/create-user! {:id (db/uuid)
                                 :email "foo@bar.com"
                                 :password "foobar"
                                 :avatar ""})
        user-2 (db/create-user! {:id (db/uuid)
                                 :email "bar@foo.com"
                                 :password "foobar"
                                 :avatar ""})
        group-1-id (db/uuid)
        group-2-id (db/uuid)
        group-1 (db/create-group! {:name "group1" :id group-1-id})
        group-2 (db/create-group! {:name "group2" :id group-2-id})
        tag-1 (db/create-tag! {:id (db/uuid) :name "tag1" :group-id (group-1 :id)})
        tag-2 (db/create-tag! {:id (db/uuid) :name "tag2" :group-id (group-2 :id)})
        tag-3 (db/create-tag! {:id (db/uuid) :name "tag3" :group-id (group-1 :id)})
        thread-1-id (db/uuid)
        thread-2-id (db/uuid)
        thread-3-id (db/uuid)
        thread-4-id (db/uuid)]

    (db/user-add-to-group! (user-1 :id) (group-1 :id))
    (db/user-subscribe-to-tag! (user-1 :id) (tag-1 :id))
    (db/user-subscribe-to-tag! (user-1 :id) (tag-3 :id))

    (db/user-add-to-group! (user-2 :id) (group-2 :id))
    (db/user-subscribe-to-tag! (user-2 :id) (tag-2 :id))

    ; this thread should be visible to user 1
    (db/create-message! {:thread-id thread-1-id :id (db/uuid)
                         :group-id group-1-id
                         :content "Hello world" :user-id (user-1 :id)
                         :created-at (java.util.Date.)})
    (db/create-message! {:thread-id thread-1-id :id (db/uuid)
                         :group-id group-1-id
                         :content "Hey world" :user-id (user-2 :id)
                         :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-1 :id)]})

    ; this thread should be visible to user 1
    (db/create-message! {:thread-id thread-2-id :id (db/uuid)
                         :group-id group-1-id
                         :content "Goodbye World" :user-id (user-2 :id)
                         :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-1 :id) (tag-3 :id)]})

    ; this thread should not be visible to user 1
    (db/create-message! {:thread-id thread-3-id :id (db/uuid)
                         :group-id group-2-id
                         :content "Hello world" :user-id (user-2 :id)
                         :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-2 :id)]})

    ; this thread should not be visible to user 1
    (db/create-message! {:thread-id thread-4-id :id (db/uuid)
                         :group-id group-2-id
                         :content "Something else" :user-id (user-2 :id)
                         :created-at (java.util.Date.)
                         :mentioned-tag-ids [(tag-2 :id)]})

    (testing "user can search by text and see threads"
      (is (= [thread-1-id]
             (search/search-threads-as (user-1 :id) ["hello" group-1-id])
             (search/search-threads-as (user-1 :id) ["HELLO" group-1-id])))
      (is (= [thread-1-id thread-2-id]
             (search/search-threads-as (user-1 :id) ["world" group-1-id])))
      (is (= () (search/search-threads-as (user-1 :id) ["something" group-1-id]))))

    (testing "can search by tag name"
      (is (= [thread-1-id thread-2-id]
             (search/search-threads-as (user-1 :id) ["#tag1" group-1-id])))
      (is (= [thread-2-id]
             (search/search-threads-as (user-1 :id) ["#tag3 world" group-1-id]))))))
