(ns braid.test.server.markdown
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [braid.server.markdown :refer [markdown->hiccup]]))

(deftest basic-changelog-markdown
  (testing
    (is (= (markdown->hiccup
             (string/join
               "\n"
               ["# Hello World"
                ""
                " - thing one"
                " - thing two"
                ""
                "## Other Heading"
                ""
                " - x"
                " - y"
                ""]))
           [:div
            [:h1 "Hello World"]
            [:ul
             [:li "thing one"]
             [:li "thing two"]]
            [:h2 "Other Heading"]
            [:ul [:li "x"] [:li "y"]]]))))
