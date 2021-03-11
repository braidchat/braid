(ns e2e.braid.e2e
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [datomic.api]
   [etaoin.xpath]
   [etaoin.api :as e]
   [etaoin.dev :as e.devtools]
   [braid.base.conf :as conf]
   [braid.dev.core :as dev.core]))

(defonce driver (e/chrome-headless {:dev {:perf {:level :all
                                                 :network? true}}}))

(defmethod etaoin.xpath/clause :fn/has-string
  [[_ text]]
  (etaoin.xpath/node-contains "string()" text))

(defn check-for [driver query]
  (e/wait-visible driver query {:timeout 1}))

(defn log [driver query]
  (println (e/get-element-text driver query)))

(defn check-requests [driver]
  (is (empty? (->> driver
                   e.devtools/get-requests
                   (filter e.devtools/request-failed?)))))

(defonce running?
  (atom false))

(defn start! []
  (dev.core/stop!)
  (dev.core/start! 5551)
  #_(datomic.api/delete-database (conf/config :db-url))
  (dev.core/seed!)
  (reset! running? true))

#_(start!)
#_(clojure.test/run-tests)

(deftest ^:test-refresh/focus e2e
  (when-not @running?
    (start!))
  (let [d driver]
    (.mkdirs (io/file "target/test-postmortems"))
    (e/with-postmortem d
      {:dir "target/test-postmortems"}

      (testing "Sending messages"
        (e/go d "http://localhost:5551")

        (testing "Log in"
          (check-for d {:tag :h1 :fn/has-string "Log in to Braid"})
          (e/fill d {:tag :input :type "email"} "foo@example.com")
          (e/fill d {:tag :input :type "password"} "foofoofoo")
          (e/click d {:tag :button :type "submit" :fn/has-string "Log in to Braid"}))

        (testing "Switch group"
          (e/click-visible d {:tag :a :fn/has-class "group" :title "Braid"}))

        (testing "View homepage"
          (check-for d {:tag :div :class "group-name" :fn/has-string "Braid"})
          (check-for d [{:tag :div :class "bar"}
                        {:tag :div :class "name" :fn/has-string "@foo"}])
          (check-for d [{:tag :div :class "card"}
                        {:tag :div :class "tag"}
                        {:tag :div :class "name" :fn/has-string "braid"}])
          (check-for d [{:tag :div :fn/has-classes [:message :seen]}
                        {:fn/has-string "Hello?"}])
          (check-for d [{:tag :div :fn/has-classes [:message :unseen]}
                        {:fn/has-string "Yep!"}]))))))
