(ns braid.core.server.handler
  (:require
   [braid.core.server.routes.api.private :refer [api-private-routes]]
   [braid.core.server.routes.api.public :refer [api-public-routes]]
   [braid.core.server.routes.api.modules :as modules]
   [braid.core.server.routes.bots :refer [bot-routes]]
   [braid.core.server.routes.client :refer [desktop-client-routes mobile-client-routes resource-routes]]
   [braid.core.server.routes.socket :refer [sync-routes]]
   [compojure.core :refer [routes context]]
   [environ.core :refer [env]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults secure-site-defaults site-defaults]]
   [ring.middleware.format :refer [wrap-restful-format]]
   [ring.middleware.edn :refer [wrap-edn-params]]))

(def session-max-age (* 60 60 24 365))

; NOT using config here, b/c it won't have started when this runs
(if (= (env :environment) "prod")
  (do
    (require 'taoensso.carmine.ring)
    (def ^:dynamic *redis-conf* {:pool {}
                                 :spec {:host "127.0.0.1"
                                        :port 6379}})
    (let [carmine-store (ns-resolve 'taoensso.carmine.ring 'carmine-store)]
      (def session-store
        (carmine-store '*redis-conf* {:expiration-secs session-max-age
                                      :key-prefix "braid"}))))
  (do
    (require 'ring.middleware.session.memory)
    (let [memory-store (ns-resolve 'ring.middleware.session.memory 'memory-store)]
      (def session-store (memory-store)))))

(defn assoc-cookie-conf [defaults]
  (-> defaults
      (assoc-in [:session :cookie-name] "braid")
      (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
      (assoc-in [:session :cookie-attrs :max-age] session-max-age)
      (assoc-in [:session :store] session-store)))

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

(def mobile-client-app
  (-> (routes
        resource-routes
        mobile-client-routes)
      (wrap-defaults (-> static-site-defaults
                         assoc-cookie-conf))))

(def desktop-client-app
  (-> (routes
        resource-routes
        desktop-client-routes)
      (wrap-defaults (-> static-site-defaults
                         assoc-cookie-conf))))

(def api-server-app
  (-> (routes
        (context "/bots" []
          bot-routes)

        (-> api-public-routes
            (wrap-defaults (-> site-defaults
                               (assoc-in [:security :anti-forgery] false)
                               assoc-cookie-conf)))

        (-> api-private-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
                               assoc-csrf-conf)))

        (-> modules/public-handler
            (wrap-defaults (-> site-defaults
                               (assoc-in [:security :anti-forgery] false)))
            (wrap-restful-format :formats [:edn :transit-json]))

        (-> modules/private-handler
            (wrap-defaults (-> site-defaults
                               assoc-cookie-conf
                               assoc-csrf-conf))
            (wrap-restful-format :formats [:edn :transit-json]))

        (-> sync-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
                               assoc-csrf-conf))))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-credentials true
                 :access-control-allow-methods [:get :put :post :delete])
      wrap-edn-params))
