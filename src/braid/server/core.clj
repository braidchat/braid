(ns braid.server.core
  (:gen-class)
  (:require
    [clojure.tools.nrepl.server :as nrepl]
    [mount.core :as mount :refer [defstate]]
    [org.httpkit.server :refer [run-server]]
    [taoensso.timbre :as timbre]
    [braid.server.email-digest :refer [email-jobs]] ; for mount
    [braid.server.sync-handler] ; for mount
    [braid.core.modules]; for mount
    [braid.server.sync] ; for multimethods and mount
    [braid.state.core] ; for mount
    [braid.server.handler :refer [mobile-client-app
                                  desktop-client-app
                                  api-server-app]]))

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
