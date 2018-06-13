(ns braid.dev.core
  "Starting namespace for Braid REPL. Utilities for dev."
  (:require
    [mount.core :as mount]
    [braid.core.modules :as modules]
    ; the following namespaces are required for their mount components:
    [braid.core]
    [braid.dev.figwheel]))

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
