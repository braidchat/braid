(ns braid.core.modules
  (:require
    [braid.core.core]
    [braid.quests.core]
    [braid.emoji.core]))

(defn init! []
  (braid.core.core/init!)
  (braid.quests.core/init!)
  (braid.emoji.core/init!))
