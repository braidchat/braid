(ns braid.core
  (:gen-class)
  (:require
    [mount.core :as mount]
    [braid.core.modules :as modules]
    ;; all following requires are for mount:
    [braid.core.server.core]
    [braid.core.server.email-digest :refer [email-jobs]]))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    ; modules must run first
    (modules/init!)
    (-> (mount/with-args {:port port})
        (mount/start {:port port}))))
