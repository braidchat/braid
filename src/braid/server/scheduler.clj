(ns braid.server.scheduler
  (:require [mount.core :as mount :refer [defstate]]
            [clojurewerkz.quartzite.scheduler :as qs]))

(defn stop-scheduler!
  [s]
  (qs/shutdown s))

(defn start-scheduler!
  []
  (println "starting quartz scheduler")
  (doto (qs/initialize)
    (qs/start)))

(defstate scheduler
  :start (start-scheduler!)
  :stop (stop-scheduler! scheduler))
