(ns braid.dev
  (:require
    [mount.core :as mount]
    ; require core so that all mount modules are pulled in
    [braid.core]))

(defn start!
  "Helper function to start Braid components in dev.
   Does not start the email worker."
  [port]
  (->
    (mount/except #{#'braid.core.server.email-digest/email-jobs})
    (mount/with-args {:port port})
    (mount/start)))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))
