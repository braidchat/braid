(ns unit.braid.core.server.middleware-test
  (:require
   [braid.core.server.middleware :refer :all]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [helpers.cookies :refer [write-cookies parse-cookie]]
   [clojure.test :refer :all]))

(deftest can-wrap-handler
  (is (fn? (wrap-universal-middleware identity {}))))

(defmulti handler :uri)
(defmethod handler "/read" [{session :session}] {:status 200 :session' session})
(defmethod handler "/write" [_] {:status 200 :session {:message "Claude E. Shannon"}})

(deftest default-session-store-is-persistent
  (let [handler (wrap-universal-middleware handler {})
        request {:uri "/write"}
        response (handler request)
        cookies (reduce merge (map parse-cookie (get-in response [:headers "Set-Cookie"])))
        request {:uri "/read" :headers {"cookie" (write-cookies cookies)}}]
    (is (= {:message "Claude E. Shannon"} ((handler request) :session')))))

(deftest custom-session-config-can-be-specified
  (let [key "0123456789012345"
        session-options {:name "session" :secure true :maxage 100000 :store (cookie-store {:key key})}
        handler (wrap-universal-middleware handler {:session session-options})
        request {:uri "/write"}
        response (handler request)
        cookies (reduce merge (map parse-cookie (get-in response [:headers "Set-Cookie"])))]
    ;; imagine the server restarts...session management configuration is reconstituted...client retains cookies...
    (let [session-options (assoc session-options :store (cookie-store {:key "0123456789012345"}))
          handler (wrap-universal-middleware handler {:session session-options})
          request {:uri "/read" :headers {"cookie" (write-cookies cookies)}}]
      (is (= {:message "Claude E. Shannon"} ((handler request) :session'))))))
