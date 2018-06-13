(ns braid.core
  (:gen-class)
  (:require
    [mount.core :as mount]
    [braid.core.modules :as modules]
    ;; all following requires are for mount:
    [braid.core.server.core]
    [braid.core.server.email-digest :refer [email-jobs]]
    [braid.core.dev.figwheel :refer [figwheel]]))

(defn -main
  "Entry point for prod"
  [& args]
  (let [port (Integer/parseInt (first args))]
    (modules/init!)
    (-> (mount/except #{#'figwheel})
        (mount/with-args {:port port})
        (mount/start {:port port}))))
