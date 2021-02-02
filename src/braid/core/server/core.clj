(ns braid.core.server.core
  (:require
   [braid.core.server.middleware :refer [wrap-universal-middleware]]
   [braid.base.conf-extra :as conf] ; to update ports
   [braid.core.server.handler :as handler]
   [braid.base.server.ws-handler] ; for mount
   [braid.base.server.jobs] ; for mount
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :refer [run-server server-port server-stop!]]
   [taoensso.timbre :as timbre]))

;; server

(defn start-server! [{:keys [middleware port]}]
  (timbre/debugf "starting desktop server on port %d" port)
  (let [server (run-server (wrap-universal-middleware #'handler/app middleware)
                           {:port port :legacy-return-value? false})
        desktop-port (server-port server)]
    (timbre/debugf "Started desktop server on port %d" desktop-port)
    (swap! conf/ports-config assoc
           :site-url (str "http://localhost:" (server-port server)))
    server))

(defn stop-server!
  [srv]
  (timbre/debugf "stopping server")
  @(server-stop! srv {:timeout 100}))

(defstate server
  :start (start-server! (mount/args))
  :stop (stop-server! server))

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
