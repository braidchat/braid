(ns braid.core
  (:gen-class)
  (:require
   [environ.core :refer [env]]
   [mount.core :as mount]
   [org.httpkit.client]
   [org.httpkit.sni-client]
   [braid.base.conf]
   [braid.core.modules :as modules]
   ;; all following requires are for mount:
   [braid.core.server.core]
   [braid.core.server.email-digest]))

;; because we often use http-kit as our http-client
;; including this so that SNI works
(alter-var-root #'org.httpkit.client/*default-client*
                (fn [_] org.httpkit.sni-client/default-client))

(defn start!
  "Entry point for prod"
  ([port]
   (start! port
           (cond-> env
             (env :aws-access-key)
             (assoc :aws/credentials-provider
                    (constantly ((juxt :aws-access-key :aws-secret-key) env))))))
  ([port args]
   ;; modules must run first
   (when (and (= (args :environment) "prod") (empty? (args :hmac-secret)))
     (println "WARNING: No :hmac-secret set, using an insecure default."))
   (modules/init! modules/default)
   (-> (mount/with-args (assoc args :port port))
       (mount/start))
   (when (zero? port)
     (mount/stop #'braid.base.conf/config)
     (mount/start #'braid.base.conf/config))))

(defn stop!
  "Helper function for stopping all Braid components."
  []
  (mount/stop))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    (start! port)))
