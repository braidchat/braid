(ns braid.core.modules
  "This namespace exist just to require other modules' defstate.
   It is required by braid.server.core"
  (:require
    [braid.core.api :as api]
    [braid.quests.server.core] ; for mount
    [mount.core :refer [defstate]]))

(defn init! [])

(defstate modules
  :start (init!))

