(ns braid.core
  (:gen-class)
  (:require
    [mount.core :as mount]
    ;; all following requires are for mount:
    [braid.core.modules]
    [braid.core.server.core]
    [braid.core.server.email-digest :refer [email-jobs]]))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    (-> (mount/with-args {:port port})
        (mount/start {:port port}))))
