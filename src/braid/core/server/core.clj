(ns braid.core.server.core
  (:require
   [braid.core.server.middleware :refer [wrap-universal-middleware]]
   [braid.core.server.handler :refer [mobile-client-app
                                      desktop-client-app
                                      api-server-app]]
   [braid.base.server.ws-handler] ; for mount
   [braid.core.server.sync] ; for multimethods
   [braid.base.server.jobs] ; for mount
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :refer [run-server server-port server-stop!]]
   [taoensso.timbre :as timbre]))

;; server

(defn- start-server!
  [server port]
  (run-server server {:port port :legacy-return-value? false}))

(defn start-servers! [opts]
  (let [wrap (fn [handler] (wrap-universal-middleware handler (opts :middleware)))
        desktop-port (:port opts)
        desktop-server (do
                         (timbre/debugf "starting desktop server on port %d" desktop-port)
                         (start-server! (wrap #'desktop-client-app) desktop-port))
        desktop-port (server-port desktop-server)]
    (timbre/debugf "Started desktop server on port %d" desktop-port)
    (let [mobile-port (inc desktop-port)
          api-port (inc mobile-port)
          mobile-server (do (timbre/debugf "starting mobile server on port %d" mobile-port)
                            (start-server! (wrap #'mobile-client-app) mobile-port))
          api-server (do (timbre/debugf "starting api server on port %d" api-port)
                         (start-server! (wrap #'api-server-app) api-port))]
      {:desktop desktop-server
       :mobile mobile-server
       :api api-server})))

(defn stop-servers!
  [srvs]
  (doseq [[typ srv] srvs]
    (timbre/debugf "stopping server %s" (name typ))
    @(server-stop! srv {:timeout 100})))

(defstate servers
  :start (start-servers! (mount/args))
  :stop (stop-servers! servers))

;; exceptions in background thread handler

(defn set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (timbre/errorf "Uncaught exception %s on %s" ex (.getName thread))
       (.printStackTrace ex)))))

(defstate thread-handler
  :start (set-default-exception-handler))
