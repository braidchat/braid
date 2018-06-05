(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    [braid.core.core]
    [braid.quests.core]
    [braid.emoji.core]
    [braid.custom-emoji.core]
    [braid.emojione-emoji.core]
    [braid.big-emoji.core]))

(defn init! []
  (hooks/reset-all!)
  (braid.core.core/init!)
  (braid.quests.core/init!)
  (braid.emoji.core/init!)
  (braid.emojione-emoji.core/init!)
  (braid.custom-emoji.core/init!)
  (braid.big-emoji.core/init!))
