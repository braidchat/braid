(ns braid.core.server.handler
  (:require
   [braid.core.server.middleware :refer [wrap-universal-middleware session-config]]
   [braid.core.server.routes.api.private :refer [api-private-routes]]
   [braid.core.server.routes.api.public :refer [api-public-routes]]
   [braid.base.server.http-api-routes :as modules]
   [braid.core.server.routes.client :refer [desktop-client-routes mobile-client-routes]]
   [braid.base.server.http-client-routes :refer [resource-routes]]
   [braid.core.server.routes.socket :refer [sync-routes]]
   [compojure.core :refer [routes context]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults secure-site-defaults site-defaults]]
   [ring.middleware.format :refer [wrap-restful-format]]
   [ring.middleware.edn :refer [wrap-edn-params]]))

(defn assoc-csrf-conf [defaults]
  (-> defaults
      (assoc-in [:security :anti-forgery] true)))

;; Here we define handlers for the various servers and subsets of the routes.  To allow late-binding/run-time configuration, we
;; intentionally excise some middleware (e.g. session handling) from the ring default middleware configurations, sometimes even excising
;; such middleware where it might not be included by default.
(def static-site-defaults
  {:session false
   :params {:urlencoded true
            :multipart  true
            :nested     true
            :keywordize true}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

(def mobile-client-app
  (-> (routes resource-routes mobile-client-routes)
      (wrap-defaults static-site-defaults)))

(def desktop-client-app
  (-> (routes resource-routes desktop-client-routes)
      (wrap-defaults static-site-defaults)))

(def api-server-app
  (-> (routes
       modules/raw-handlers

       (-> api-public-routes
           (wrap-defaults (-> site-defaults
                              (assoc :session false)
                              (assoc-in [:security :anti-forgery] false))))

       (-> modules/public-handler
           (wrap-defaults (-> site-defaults
                              (assoc :session false)
                              (assoc-in [:security :anti-forgery] false)))
           (wrap-restful-format :formats [:edn :transit-json]))

       (-> sync-routes
           (wrap-defaults (-> api-defaults
                              (assoc :session false)
                              assoc-csrf-conf)))

       (-> api-private-routes
           (wrap-defaults (-> api-defaults
                              (assoc :session false)
                              assoc-csrf-conf)))

       ;; this needs to be last, because the middleware will return
       ;; 401 if not authorized & hence not fall-through to other
       ;; routes
       (-> modules/private-handler
           (wrap-defaults (-> site-defaults
                              (assoc :session false)
                              assoc-csrf-conf))
           (wrap-restful-format :formats [:edn :transit-json])))
      wrap-edn-params))
