(ns integration.braid.system-start-stop-test
  (:require [braid.core :refer :all]
            [clojure.test :refer :all]))

(deftest start-stop
  (is (start! 0))
  (is (stop!)))
