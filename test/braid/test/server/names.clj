(ns braid.test.server.names
  (:require [clojure.test :refer :all]
            [braid.common.util :as util]))

(deftest valid-user-names
  (testing "Names can contain any letters, digits, hypens, and underscores"
    (is (util/valid-nickname? "james"))
    (is (util/valid-nickname? "9james"))
    (is (util/valid-nickname? "9jam-es"))
    (is (util/valid-nickname? "💩"))
    (is (not (util/valid-nickname? "9j_/am-es")))
    (is (not (util/valid-nickname? " ")))
    (is (not (util/valid-nickname? "  ")))
    (is (not (util/valid-nickname? "\t")))
    (is (not (util/valid-nickname? "\n")))
    (is (not (util/valid-nickname? "james.a")))
    (is (not (util/valid-nickname? "james a")))
    (is (not (util/valid-nickname? "james.;a"))))
  (testing "Names must be at least one and less than 30 characters"
    (is (util/valid-nickname? "012345678901234567890123456789"))
    (is (not (util/valid-nickname? "")))
    (is (not (util/valid-nickname? "012345678901234567890123456789z")))
    (is (util/valid-nickname? "foobarbazquux"))))

(deftest valid-tag-names
  (testing "Names can contain any letters, digits, hypens, underscores, and slashes"
    (is (util/valid-tag-name? "stuf"))
    (is (util/valid-tag-name? "stuf9"))
    (is (util/valid-tag-name? "stuff-9"))
    (is (util/valid-tag-name? "stuff_9"))
    (is (util/valid-tag-name? "💩"))
    (is (util/valid-tag-name? "blah/baz"))
    (is (not (util/valid-tag-name? " ")))
    (is (not (util/valid-tag-name? "  ")))
    (is (not (util/valid-tag-name? "\t")))
    (is (not (util/valid-tag-name? "\n")))
    (is (not (util/valid-tag-name? "tag.a")))
    (is (not (util/valid-tag-name? "tag a")))
    (is (not (util/valid-tag-name? "tag;a"))))
  (testing "Names must be at least one and less than 30 characters"
    (is (util/valid-tag-name? "012345678901234567890123456789"))
    (is (not (util/valid-tag-name? "")))
    (is (util/valid-tag-name? "01234567890123456789012345678901234567890123456789"))
    (is (not (util/valid-tag-name? "0123456789012345678901234567890123456789012345678901")))
    (is (util/valid-tag-name? "foobarbazquux"))))
