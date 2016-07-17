(ns braid.test.server.markdown
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [braid.server.markdown :refer [markdown->hiccup]]))

(deftest basic-changelog-markdown
  (testing "Simple markdown parsing"
    (is (= (markdown->hiccup
             (string/join
               "\n"
               ["# Hello World"
                ""
                " - thing one"
                " - thing two"
                ""
                "## Other Heading ##"
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
            [:ul [:li "x"] [:li "y"]]]))
    (testing "with links"
      (is (= (markdown->hiccup
               (string/join
                 "\n"
                 ["# Hello [there](./foo.png) World"
                  ""
                  " - thing [one](http://foo.com)!"
                  " - thing two"
                  ""
                  "## Other Heading ##"
                  ""
                  " * x"
                  " * y"
                  ""]))
             [:div
              [:h1 "Hello " [:a {:href "./foo.png"} "there"] " World"]
              [:ul
               [:li "thing " [:a {:href "http://foo.com"} "one"] "!"]
               [:li "thing two"]]
              [:h2 "Other Heading"]
              [:ul [:li "x"] [:li "y"]]]))))
  (testing "paragraphs"
    (is (= (markdown->hiccup
             (string/join
               "\n"
               ["# Hello All #"
                ""
                "This is a paragraph"
                "split over multiple lines"
                "with [links](http://foo.com) in it"
                ""
                "This is a new paragraph"
                ""
                "  - and a"
                "  - list of"
                "  - items"]))
           [:div
            [:h1 "Hello All"]
            [:p "This is a paragraph"
             "split over multiple lines"
             "with " [:a {:href "http://foo.com"} "links"] " in it"]
            [:p "This is a new paragraph"]
            [:ul
             [:li "and a"]
             [:li "list of"]
             [:li "items"]]]))))
