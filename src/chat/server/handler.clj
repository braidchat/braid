(ns chat.server.handler
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [mount.core :as mount :refer [defstate]]
            [compojure.core :refer [routes]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults
                                              secure-site-defaults site-defaults]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [get-header]]
            [taoensso.timbre :as timbre]
            [clojure.tools.nrepl.server :as nrepl]
            [chat.server.sync :as sync :refer [sync-routes router]]
            [chat.server.routes :as routes
             :refer [desktop-client-routes
                     mobile-client-routes
                     api-private-routes
                     api-public-routes
                     resource-routes
                     extension-routes]]
            [chat.server.conf :as conf]
            [environ.core :refer [env]]
            ; just requiring to register multimethods
            chat.server.extensions.asana
            [chat.server.email-digest :refer [email-jobs]]))

(if (= (env :environment) "prod")
  (do
    (require 'taoensso.carmine.ring)
    (def ^:dynamic *redis-conf* {:pool {}
                                 :spec {:host "127.0.0.1"
                                        :port 6379}})
    (let [carmine-store (ns-resolve 'taoensso.carmine.ring 'carmine-store)]
      (def session-store
        (carmine-store '*redis-conf* {:expiration-secs (* 60 60 24 7)
                                      :key-prefix "braid"}))))
  (do
    (require 'ring.middleware.session.memory)
    (let [memory-store (ns-resolve 'ring.middleware.session.memory 'memory-store)]
      (def session-store (memory-store)))))

(defn assoc-cookie-conf [defaults]
  (-> defaults
      (assoc-in [:session :cookie-attrs :secure] (= (env :environment) "prod"))
      (assoc-in [:session :cookie-attrs :max-age] (* 60 60 24 7))
      (assoc-in [:session :store] session-store)))

(defn assoc-csrf-conf [defaults]
  (-> defaults
      (assoc-in [:security :anti-forgery] true)))

(def static-site-defaults
  {:static {:resources "public"}
   :params {:urlencoded true
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
      (wrap-defaults static-site-defaults)))

(def desktop-client-app
  (-> (routes
        resource-routes
        desktop-client-routes)
      (wrap-defaults static-site-defaults)))

(def api-server-app
  (-> (routes
        (-> api-public-routes
            (wrap-defaults (-> site-defaults
                               (assoc-in [:security :anti-forgery] false)
                               assoc-cookie-conf)))

        (-> extension-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
                               )))
        (-> api-private-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
                               assoc-csrf-conf
                               )))
        (-> sync-routes
            (wrap-defaults (-> api-defaults
                               assoc-cookie-conf
                               assoc-csrf-conf
                               ))))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-credentials true
                 :access-control-allow-methods [:get :put :post :delete])
      wrap-edn-params))

;; server

(defn start-server!
  [type port]
  (let [app (case type
              :api #'api-server-app
              :desktop #'desktop-client-app
              :mobile #'mobile-client-app)]
    (run-server app {:port port})))

(defn start-servers! []
  (let [desktop-port (:port (mount/args))
        mobile-port (inc desktop-port)
        api-port (inc mobile-port)
        desktop-server (do
                         (println "starting desktop client on port " desktop-port)
                         (start-server! :desktop desktop-port))
        mobile-server (do (println "starting mobile client on port " mobile-port)
                          (start-server! :mobile mobile-port))
        api-server (do (reset! conf/api-port api-port)
                       (println "starting api on port " api-port)
                       (start-server! :api api-port))]
    {:desktop desktop-server
     :mobile mobile-server
     :api api-server}))

(defn stop-servers!
  [srvs]
  (doseq [[typ srv] srvs]
    (println "stopping server " (name typ))
    (srv :timeout 100)))

(defstate servers
  :start (start-servers!)
  :stop (stop-servers! servers))

;; nrepl

(defstate nrepl
  :start (nrepl/start-server :port (:repl-port (mount/args)))
  :stop (nrepl/stop-server nrepl))

;; exceptions in background thread handler

(defn set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (timbre/errorf "Uncaught exception %s on %s" ex (.getName thread))))))

(defstate thread-handler
  :start (set-default-exception-handler))

;; main
(defn dev-main
  "Start things up, but don't start the email server or nrepl"
  [port]
  (->
    (mount/except #{#'email-jobs #'nrepl})
    (mount/with-args {:port port})
    (mount/start)))

(defn -main  [& args]
  (let [port (Integer/parseInt (first args))
        repl-port (Integer/parseInt (second args))]
    (mount/start-with-args {:port port
                            :repl-port repl-port})))
