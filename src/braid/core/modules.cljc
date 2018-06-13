(ns braid.core.modules
  (:require
    [mount.core :refer [defstate]]
    [braid.core.hooks :as hooks]
    ; modules:
    [braid.big-emoji.core]
    [braid.core.core]
    [braid.custom-emoji.core]
    [braid.embeds.core]
    [braid.emoji.core]
    [braid.emojione-emoji.core]
    [braid.image-embeds.core]
    [braid.permalinks.core]
    [braid.quests.core]
    [braid.stars.core]
    [braid.video-embeds.core]
    [braid.website-embeds.core]))

(defn init! []
  (hooks/reset-all!)

  (braid.big-emoji.core/init!)
  (braid.custom-emoji.core/init!)
  (braid.core.core/init!)
  (braid.embeds.core/init!)
  (braid.emoji.core/init!)
  (braid.emojione-emoji.core/init!)
  (braid.image-embeds.core/init!)
  (braid.permalinks.core/init!)
  (braid.quests.core/init!)
  (braid.stars.core/init!)
  (braid.video-embeds.core/init!)
  (braid.website-embeds.core/init!))

(defstate modules
  :start (init!))
