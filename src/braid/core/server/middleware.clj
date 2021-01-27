(ns braid.core.server.middleware
  (:require
   [braid.base.conf :refer [config]]
   [ring.middleware.session :refer [wrap-session]]
   [clojure.stacktrace]
   [taoensso.timbre :as timbre]))

(def session-max-age (* 60 60 24 365))

(def session-store
  (delay
    (if (config :redis-uri)
      (do
        (require 'taoensso.carmine.ring)
        (let [carmine-store (ns-resolve 'taoensso.carmine.ring 'carmine-store)
              redis-conf {:pool {}
                          :spec {:uri (config :redis-uri)}} ]
          (carmine-store redis-conf {:expiration-secs session-max-age
                                     :key-prefix "braid"})))
      (do
        (require 'ring.middleware.session.cookie)
        (let [cookie-store (ns-resolve 'ring.middleware.session.cookie
                                       'cookie-store)]
          (cookie-store))))))

(def session-config
  (delay {:cookie-name "braid",
          :cookie-attrs {:secure (boolean (config :cookie-secure))
                         :max-age session-max-age},
          :store @session-store}))

(defn- wrap-log-requests
  [handler]
  (fn [{uri :uri method :request-method :as request}]
    (let [t0 (System/currentTimeMillis)
          {:keys [status] :as response} (handler request)]
      (timbre/debugf "[%s +%4dms] %8.8s %s"
                     status (- (System/currentTimeMillis) t0) method uri)
      response)))

(letfn [(e-response [e] (let [stacktrace (with-out-str (clojure.stacktrace/print-cause-trace e))]
                          (assoc-in {:status 500
                                     :headers {"Content-Type" "text/plain; charset=us-ascii"}
                                     :body (format "We're sorry, something went wrong.\n%s" stacktrace)}
                                    [:headers "X-Exception"] (.getMessage e))))]
  (defn wrap-failsafe
    "Last ditch handler for exceptions" ;; NB: Don't do anything fancy here
    [handler]
    (fn failsafe
      ([request]
       (try (handler request)
            (catch Throwable e
              (timbre/error e "Failsafe handler caught a Throwable")
              (e-response e))))
      ([request respond raise]
       (try (handler request respond raise)
            (catch Throwable e
              (timbre/error e "Failsafe handler caught a Throwable")
              (respond (e-response e))))))))

;; Here we define the universal (as opposed to route-specific) middleware stack.
(defn wrap-universal-middleware
  "Wrap the handler with middleware that is universally applicable across the site."
  [handler {:keys [session]}]
  (-> handler
      (wrap-session (or session @session-config))
      wrap-log-requests
      wrap-failsafe))
