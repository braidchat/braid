(ns braid.core.server.core
  (:require
    [braid.core.server.handler :refer [mobile-client-app
                                       desktop-client-app
                                       api-server-app]]
    [braid.base.server.ws-handler] ; for mount
    [braid.core.server.sync] ; for multimethods
    [braid.base.server.jobs] ; for mount
    [mount.core :as mount :refer [defstate]]
    [org.httpkit.server :refer [run-server]]
    [taoensso.timbre :as timbre]))

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
        api-server (do (println "starting api on port " api-port)
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

