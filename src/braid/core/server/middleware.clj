(ns braid.core.server.middleware
  (:require
   [environ.core :refer [env]]
   [ring.middleware.session :refer [wrap-session]]
   [taoensso.timbre :as timbre]))

(def session-max-age (* 60 60 24 365))

;; NOT using config here, b/c it won't have started when this runs
(if (env :redis-uri)
  (do
    (require 'taoensso.carmine.ring)
    (def ^:dynamic *redis-conf* {:pool {}
                                 :spec {:uri (env :redis-uri)}})
    (let [carmine-store (ns-resolve 'taoensso.carmine.ring 'carmine-store)]
      (def session-store
        (carmine-store '*redis-conf* {:expiration-secs session-max-age
                                      :key-prefix "braid"}))))
  (do
    (require 'ring.middleware.session.cookie)
    (let [cookie-store (ns-resolve 'ring.middleware.session.cookie 'cookie-store)]
      (def session-store (cookie-store)))))

(def session-config
  {:cookie-name "braid",
   :cookie-attrs {:secure (cond
                            (env :http-only) false
                            (= (env :environment) "prod") true
                            :else false)
                  :max-age session-max-age},
   :store session-store})

(defn- wrap-log-requests
  [handler]
  (fn [{uri :uri method :request-method :as request}]
    (let [t0 (System/currentTimeMillis)
          {:keys [status] :as response} (handler request)]
      (timbre/debugf "[%s +%4dms] %8.8s %s" status (- (System/currentTimeMillis) t0) method uri)
      response)))

;; Here we define the universal (as opposed to route-specific) middleware stack.
(defn wrap-universal-middleware
  "Wrap the handler with middleware that is universally applicable across the site."
  [handler {:keys [session] :or {session session-config}}]
  (-> handler
      (wrap-session session)
      wrap-log-requests))
