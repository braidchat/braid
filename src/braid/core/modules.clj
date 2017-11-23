(ns braid.core.modules
  (:require
    [braid.core.api :as api]
    [braid.state.core]
    [braid.server.schema]
    [braid.server.sync]
    [braid.quests.server.core]))

(defn init! []
  (braid.state.core/init!)
  (braid.server.schema/init!)
  (braid.server.sync/init!)
  (braid.quests.server.core/init!))

