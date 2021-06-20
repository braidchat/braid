(ns e2e.braid.e2e
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [datomic.api]
   [etaoin.xpath]
   [etaoin.api :as e]
   [etaoin.dev :as e.devtools]
   [etaoin.keys :as e.keys]
   [braid.base.conf :as conf]
   [braid.dev.core :as dev.core]
   [braid.test.lib.xpath :refer [css->xpath]]
   [braid.search.lucene :as lucene]))

(defn e2e-fixture
  [t]
  (when (map? conf/config)
    (dev.core/drop-db!)
    (reset! lucene/-store nil)
    (dev.core/stop!))
  (dev.core/start! 5551)
  (dev.core/seed!)
  (dev.core/disable-request-logging!)
  (t))

(use-fixtures :once e2e-fixture)

(defonce driver (e/chrome-headless {:dev {:perf {:level :all
                                                 :network? true}}}))

(defmethod etaoin.xpath/clause :fn/has-string
  [[_ text]]
  (etaoin.xpath/node-contains "string()" text))

(defn check-for [driver query]
  (e/wait-visible driver query {:timeout 2}))

(defn check-not [driver query]
  (e/wait-absent driver query {:timeout 2}))

(defn xpath-contains-string
  [css-selector string]
  (->> string
       (format "[contains(. , \"%s\")]")
       (str (css->xpath css-selector))))

(defn check-requests [driver]
  (is (empty? (->> driver
                   e.devtools/get-requests
                   (filter e.devtools/request-failed?)))))

(defonce keyboard (e/make-key-input))

(defn press-key
  [driver key]
  (->> (e/add-key-press keyboard key)
       (e/perform-actions driver)))

(deftest ^:test-refresh/focus e2e
  (let [d driver]
    (.mkdirs (io/file "target/test-postmortems"))
    (e/with-postmortem d
      {:dir "target/test-postmortems"}

      (testing "Sending messages"
        (e/go d "http://localhost:5551")
        (check-requests d)

        (testing "Log in as @foo"
          (check-for d {:tag :h1 :fn/has-string "Log in to Braid"})
          (e/fill d {:tag :input :type "email"} "foo@example.com")
          (e/fill d {:tag :input :type "password"} "foofoofoo")
          (e/click d {:tag :button :type "submit" :fn/has-string "Log in to Braid"})
          (check-requests d))

        (testing "Switch group"
          (e/click-visible d {:tag :a :fn/has-class "group" :title "Braid"})
          (check-requests d))

        (testing "View homepage"
          (check-for d {:xpath (xpath-contains-string ".group-header .group-name"
                                                      "Braid")})
          (check-for d {:xpath (xpath-contains-string ".user-header .name"
                                                      "@foo")})
          (check-for d {:xpath (xpath-contains-string ".card .tag .name"
                                                      "#braid")})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Hello?")})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Yep!")}))

        (testing "Send messages in existing thread"
          (e/fill d [{:class "textarea"} {:tag :textarea}]
                  "Yo @bar buddy!")
          (press-key d e.keys/enter)
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "buddy!")})
          (check-for d {:xpath (xpath-contains-string ".content .dummy .user .name"
                                                      "@bar")})
          (e/fill d [{:class "textarea"} {:tag :textarea}]
                  "Long time no talk!!")
          (press-key d e.keys/enter)
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Long time no talk!!")})
          (check-requests d))

        (testing "Send messages in a new thread"
          (e/click d {:tag :button :class "new-thread"})
          (e/fill-human d {:tag :textarea :index 1} "Welcome to #new-thread")
          (e/wait d 1) ; for tag creation to work
          (press-key d e.keys/enter)
          (press-key d e.keys/enter)
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (e/fill-human d {:tag :textarea :index 1} "This thread is gonna be awesome!")
          (press-key d e.keys/enter)
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "This thread is gonna be awesome!")})
          (press-key d e.keys/enter)
          (check-requests d))

        (testing "Log out"
          (e/mouse-move-to d {:tag :div :class "more"})
          (e/click d {:tag :a :fn/has-string "Log Out"})
          (check-for d {:tag :h1 :fn/has-string "Log in to Braid"})
          (check-requests d)))

      (testing "Receiving messages"
        (testing "Log in as @bar"
          (e/fill d {:tag :input :type "email"} "bar@example.com")
          (e/fill d {:tag :input :type "password"} "barbarbar")
          (e/click d {:tag :button :type "submit" :fn/has-string "Log in to Braid"})
          (check-for d {:xpath (xpath-contains-string ".user-header .name"
                                                      "@bar")})
          (check-requests d))

        (testing "Switch group"
          (e/click-visible d {:tag :a :fn/has-class "group" :title "Braid"})
          (check-requests d))

        (testing "View received messages"
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Long time no talk!")})
          (check-for d {:xpath (xpath-contains-string ".content .dummy .user .name"
                                                      "@bar")})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "This thread is gonna be awesome!")})))

      (testing "Closing and bringing back threads"
        (testing "Close a thread"
          (e/click d {:xpath (css->xpath ".close")})
          (check-not d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (check-requests d))

        (testing "Bring back the closed thread from Recent Page"
          (e/click d {:tag :a :title "Recently Closed"})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (e/click d {:tag :div :title "Mark Unread"})
          (check-not d {:xpath (css->xpath ".messages")})
          (check-requests d))

        (testing "See the thread back on Inbox Page"
          (e/click d {:tag :a :title "Inbox"})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (check-requests d)))

      (testing "Starring threads"
        (testing "Star a thread"
          (e/click d {:tag :div :class "star not-starred"})
          (check-for d {:tag :div :class "star starred"})
          (check-requests d))

        (testing "See starred thread on Starred Threads Page"
          (e/click d {:tag :a :title "Starred Threads"})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})
          (check-requests d)))

      (testing "Searching for threads"
        (testing "Search for a word"
          (e/fill-human d [{:class "search-bar"} {:tag :input :type "text"}]
                        "Welcome")
          (check-requests d))

        (testing "See results"
          (check-for d {:xpath (xpath-contains-string ".content .description"
                                                      "Displaying 1/1")})
          (check-for d {:xpath (xpath-contains-string ".message .content"
                                                      "Welcome to #new-thread")})))
      (testing "Unsubscribing from tags"
        (testing "Unsubscribe from a tag"
          (e/click d {:tag :a :title "Inbox"})
          (e/click d {:xpath (css->xpath ".close")})
          (e/mouse-move-to d {:tag :div :class "more"})
          (e/click d {:tag :a :fn/has-string "Manage Subscriptions"})
          (e/js-execute d "[... document.querySelectorAll('.tag .name')].filter(el => el.outerText === '#NEW-THREAD')[0].parentElement.parentElement.parentElement.querySelector('a.button').click()")
          (check-requests d))

        (testing "Start a thread with that tag as @foo"
          (check-for d {:tag :div :class "more"})
          (e/mouse-move-to d {:tag :div :class "more"})
          (e/click d {:tag :a :fn/has-string "Log Out"})
          (check-for d {:tag :input :type "email"})
          (e/fill d {:tag :input :type "email"} "foo@example.com")
          (e/fill d {:tag :input :type "password"} "foofoofoo")
          (e/click d {:tag :button :type "submit" :fn/has-string "Log in to Braid"})
          (e/click-visible d {:tag :a :fn/has-class "group" :title "Braid"})
          (e/click d {:tag :button :class "new-thread"})
          (e/fill-human d {:tag :textarea :index 1} "#new-thread is super cool!")
          (press-key d e.keys/enter))

        (testing "Can't see the new thread with unsubscribed tag as @bar"
          (check-for d {:tag :div :class "more"})
          (e/mouse-move-to d {:tag :div :class "more"})
          (e/click d {:tag :a :fn/has-string "Log Out"})
          (check-for d {:tag :input :type "email"})
          (e/fill d {:tag :input :type "email"} "bar@example.com")
          (e/fill d {:tag :input :type "password"} "barbarbar")
          (e/click d {:tag :button :type "submit" :fn/has-string "Log in to Braid"})
          (e/click-visible d {:tag :a :fn/has-class "group" :title "Braid"})
          (check-not d {:xpath (xpath-contains-string ".message .content"
                                                      "#new-thread is super cool!")})
          (check-requests d))))))
