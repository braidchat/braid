(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    [braid.core.core]
    [braid.quests.core]
    [braid.emoji.core]))

(defn init! []
  (hooks/reset-all!)
  (braid.core.core/init!)
  (braid.quests.core/init!)
  (braid.emoji.core/init!))
