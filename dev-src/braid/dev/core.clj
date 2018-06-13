(ns braid.dev.core
  "Starting namespace for Braid REPL. Utilities for dev."
  (:require
    [mount.core :as mount]
    [braid.core.server.seed :as seed]
    [braid.core.modules :as modules]
    ; the following namespaces are required for their mount components:
    [braid.core]
    [braid.dev.figwheel]))

(defn seed!
  "Seed the database.
   Database connection must be started prior to seeding, so be sure to call start! first."
  []
  (seed/seed!))

(defn start!
  "Helper function to start Braid components in dev.
   Does not start the email worker."
  [port]
  ; modules must run first
  (modules/init!)
  (-> (mount/except #{#'braid.core.server.email-digest/email-jobs})
      (mount/with-args {:port port})
      (mount/start)))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))
