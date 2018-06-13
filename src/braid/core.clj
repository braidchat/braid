(ns braid.core
  (:gen-class)
  (:require
    [mount.core :as mount]
    [braid.core.modules :as modules]
    [braid.core.server.core] ; for mount
    [braid.core.server.email-digest :refer [email-jobs]] ; for mount
    ))

(defn start!
  "Helper function to start Braid components in dev.
   Does not start the email worker."
  [port]
  (modules/init!)
  (->
    (mount/except #{#'email-jobs})
    (mount/with-args {:port port})
    (mount/start)))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    (modules/init!)
    (mount/start-with-args {:port port})))
