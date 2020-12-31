(ns braid.core.server.core
  (:require
   [braid.base.conf-extra :as conf] ; to update ports
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

(defn start-server!
  [type port]
  (let [app (case type
              :api #'api-server-app
              :desktop #'desktop-client-app
              :mobile #'mobile-client-app)]
    (run-server app {:port port :legacy-return-value? false})))

(defn start-servers! []
  (let [desktop-port (:port (mount/args))
        desktop-server (do
                         (timbre/debugf "starting desktop server on port %d" desktop-port)
                         (start-server! :desktop desktop-port))
        desktop-port (server-port desktop-server)]
    (timbre/debugf "Started desktop server on port %d" desktop-port)
    (let [mobile-port (inc desktop-port)
          api-port (inc mobile-port)
          mobile-server (do (timbre/debugf "starting mobile server on port %d" mobile-port)
                            (start-server! :mobile mobile-port))
          api-server (do (timbre/debugf "starting api server on port %d" api-port)
                         (start-server! :api api-port))]
      (swap! conf/ports-config assoc
             :api-domain (str "localhost:" (server-port api-server)))
      (swap! conf/ports-config assoc
             :site-url (str "http://localhost:" (server-port desktop-server)))
      {:desktop desktop-server
       :mobile mobile-server
       :api api-server})))

(defn stop-servers!
  [srvs]
  (doseq [[typ srv] srvs]
    (timbre/debugf "stopping server %s" (name typ))
    @(server-stop! srv {:timeout 100})))

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
