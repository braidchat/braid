(ns braid.core.server.handler
  (:require
   [braid.core.server.routes.api.private :refer [api-private-routes]]
   [braid.core.server.routes.api.public :refer [api-public-routes]]
   [braid.base.server.http-api-routes :as modules]
   [braid.core.server.routes.client :refer [desktop-client-routes mobile-client-routes]]
   [braid.base.server.http-client-routes :refer [resource-routes]]
   [braid.core.server.routes.socket :refer [sync-routes]]
   [compojure.core :refer [routes context]]
   [environ.core :refer [env]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults secure-site-defaults site-defaults]]
   [ring.middleware.format :refer [wrap-restful-format]]
   [ring.middleware.edn :refer [wrap-edn-params]]
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

(defn assoc-csrf-conf [defaults]
  (-> defaults
      (assoc-in [:security :anti-forgery] true)))

(def static-site-defaults
  {:params {:urlencoded true
            :multipart  true
            :nested     true
            :keywordize true}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

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

;; Here we define universal (as opposed to route-specific) middleware.
(defn- wrap-universal-middleware
  "Wrap the handler with middleware that is universally applicable across the site."
  [handler options]
  (-> handler
      (wrap-session (options :session))
      wrap-log-requests))

(def mobile-client-app
  (-> (routes resource-routes mobile-client-routes)
      (wrap-defaults static-site-defaults)
      (wrap-universal-middleware {:session session-config})))

(def desktop-client-app
  (-> (routes resource-routes desktop-client-routes)
      (wrap-defaults static-site-defaults)
      (wrap-universal-middleware {:session session-config})))

(def api-server-app
  (-> (routes
       modules/raw-handlers

       (-> api-public-routes
           (wrap-defaults (-> site-defaults
                              (assoc-in [:security :anti-forgery] false))))

       (-> modules/public-handler
           (wrap-defaults (-> site-defaults
                              (assoc-in [:security :anti-forgery] false)))
           (wrap-restful-format :formats [:edn :transit-json]))

       (-> sync-routes
           (wrap-defaults (-> api-defaults assoc-csrf-conf)))

       (-> api-private-routes
           (wrap-defaults (-> api-defaults assoc-csrf-conf)))

       ;; this needs to be last, because the middleware will return
       ;; 401 if not authorized & hence not fall-through to other
       ;; routes
       (-> modules/private-handler
           (wrap-defaults (-> site-defaults assoc-csrf-conf))
           (wrap-restful-format :formats [:edn :transit-json])))
      (wrap-cors :access-control-allow-origin (->>
                                               [(when-let [site (env :site-url)]
                                                  (re-pattern (java.util.regex.Pattern/quote site)))
                                                (when-let [mobile-site (env :mobile-site-url)]
                                                  (re-pattern (java.util.regex.Pattern/quote mobile-site)))
                                                #"http://localhost:\d+"]
                                               (remove nil?))
                 :access-control-allow-credentials true
                 :access-control-allow-methods [:get :put :post :delete])
      wrap-edn-params
      (wrap-universal-middleware {:session session-config})))
