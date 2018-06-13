(ns braid.core.dev.figwheel
  (:require
    [figwheel-sidecar.repl-api :as ra]
    [mount.core :refer [defstate]]))

(defn repl
  "Start a clojurescript repl"
  []
  (ra/cljs-repl))

(defstate figwheel
  :start (ra/start-figwheel!)
  :stop (ra/stop-figwheel!))
