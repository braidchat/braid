(ns braid.core.server.middleware
  (:require
   [braid.base.conf :refer [config]]
   [mount.core :as mount :refer [defstate]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.session :refer [wrap-session]]
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
          :cookie-attrs {:secure (cond
                                   (config :http-only) false
                                   (= (config :environment) "prod") true
                                   :else false)
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

;; Here we define the universal (as opposed to route-specific) middleware stack.
(defn wrap-universal-middleware
  "Wrap the handler with middleware that is universally applicable across the site."
  [handler {:keys [session]}]
  (-> handler
      (wrap-cors :access-control-allow-origin
                 (->>
                   [(when-let [site (config :site-url)]
                      (re-pattern (java.util.regex.Pattern/quote site)))
                    (when-let [mobile-site (config :mobile-site-url)]
                      (re-pattern (java.util.regex.Pattern/quote mobile-site)))
                    #"http://localhost:\d+"]
                   (remove nil?))
                 :access-control-allow-credentials true
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-session (or session @session-config))
      wrap-log-requests))
