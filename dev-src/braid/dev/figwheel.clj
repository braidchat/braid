(ns braid.dev.figwheel
  (:require
    [figwheel.main.api :as repl-api]
    [mount.core :refer [defstate]]))

(defstate figwheel
  :start (repl-api/start
           {:mode :serve
            :open-url false
            :ring-server-options {:port 3559}
            :connect-url "ws://[[client-hostname]]:[[server-port]]/figwheel-connect"
            :watch-dirs ["src"]}
           "dev")
  :stop (repl-api/stop-all))


