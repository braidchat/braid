(ns braid.test.client.ui.views.message-test
  "Unit tests for functions defined by the
  braid.client.ui.views.message namespace"
  (:require
   [braid.client.ui.views.message :as message]
   [cljs.test :refer-macros [deftest is testing]]))

(deftest test-abridged-url
  (testing "Returns domain and path as-is when then are less than 30 chars"
    (is (= "localhost"
           (message/abridged-url "https://localhost:5555")))
    (is (= "braid.chat"
           (message/abridged-url "https://braid.chat")))
    (is (= "github.com/some-user/29-chars"
           (message/abridged-url "https://github.com:1234/some-user/29-chars?arg=1"))))
  (testing "Returns domain + abridged path when they are not less than 30 chars"
    (is (= "short.domain/...ally/long/path"
           (message/abridged-url "https://short.domain/really/and/i/mean/really/long/path"))))
  (testing "Returns domain + elipsis when the domain is not less than 30 chars"
    (is (= "really.and.i.mean.really.long.domain/..."
           (message/abridged-url "https://really.and.i.mean.really.long.domain/short/path")))))
