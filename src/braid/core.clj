(ns braid.core
  (:gen-class)
  (:require
   [mount.core :as mount]
   [org.httpkit.client]
   [org.httpkit.sni-client]
   [braid.core.modules :as modules]
   ;; all following requires are for mount:
   [braid.core.server.core]
   [braid.core.server.email-digest :refer [email-jobs]]))

;; because we often use http-kit as our http-client
;; including this so that SNI works
(alter-var-root #'org.httpkit.client/*default-client*
                (fn [_] org.httpkit.sni-client/default-client))

(defn start!
  "Entry point for prod"
  [port]
  ;; modules must run first
  (modules/init!)
  (-> (mount/with-args {:port port})
      (mount/start)))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    (start! port)))
