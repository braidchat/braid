(ns braid.core.modules
  (:require
    [braid.core.api :as api]
    [braid.quests.server.core]))

(defn init! []
  (braid.quests.server.core/init!))

