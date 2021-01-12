(ns unit.braid.core.server.middleware-test
  (:require
   [braid.core.server.middleware :refer :all]
   [braid.test.fixtures.conf :refer [start-config]]
   [clojure.test :refer :all]
   [helpers.cookies :refer [write-cookies parse-cookie]]
   [ring.middleware.session.cookie :refer [cookie-store]]))

(use-fixtures :each start-config)

(deftest can-wrap-handler
  (is (fn? (wrap-universal-middleware identity {}))))

(defmulti handler :uri)
(defmethod handler "/read" [{session :session}] {:status 200 :session' session})
(defmethod handler "/write" [_] {:status 200 :session {:message "Claude E. Shannon"}})
(defmethod handler "/throw" [_] (throw (Exception. "WFT")))

(deftest default-session-store-is-persistent
  (let [handler (wrap-universal-middleware handler {})
        request {:uri "/write"}
        response (handler request)
        cookies (reduce merge (map parse-cookie (get-in response [:headers "Set-Cookie"])))
        request {:request-method :get :uri "/read" :headers {"cookie" (write-cookies cookies)}}]
    (is (= {:message "Claude E. Shannon"} ((handler request) :session')))))

(deftest custom-session-config-can-be-specified
  (let [key "0123456789012345"
        session-options {:cookie-name "session" :cookie-attrs {:http-only true :secure false :max-age (* 60 60)} :store (cookie-store {:key key})}
        handler (wrap-universal-middleware handler {:session session-options})
        request {:request-method :get :uri "/write"}
        response (handler request)
        cookies (reduce merge (map parse-cookie (get-in response [:headers "Set-Cookie"])))]
    (is (map? (cookies "session")))
    ;; imagine the server restarts...session management configuration is reconstituted...client retains cookies...
    (let [session-options (assoc session-options :store (cookie-store {:key key}))
          handler (wrap-universal-middleware handler {:session session-options})
          request {:request-method :get :uri "/read" :headers {"cookie" (write-cookies cookies)}}]
      (is (= {:message "Claude E. Shannon"} ((handler request) :session'))))))

(deftest exceptions-are-handled
  (let [handler (wrap-universal-middleware handler {})
        request {:uri "/throw"}
        expected {:status 500
	          :headers {"Content-Type" "text/plain; charset=us-ascii", "X-Exception" "WFT"}
	          :body #"(?is).*something went wrong.*java.lang.Exception.*"}]
    (is (= (select-keys expected [:status :headers]) (select-keys (handler request) [:status :headers])))
    (is (re-matches (expected :body) ((handler request) :body)))))
