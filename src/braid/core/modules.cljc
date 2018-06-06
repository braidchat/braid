(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    [braid.core.core]
    [braid.quests.core]
    [braid.emoji.core]
    [braid.custom-emoji.core]
    [braid.emojione-emoji.core]
    [braid.big-emoji.core]
    [braid.embeds.core]
    [braid.video-embeds.core]
    [braid.image-embeds.core]
    [braid.website-embeds.core]))

(defn init! []
  (hooks/reset-all!)
  (braid.core.core/init!)
  (braid.quests.core/init!)
  (braid.emoji.core/init!)
  (braid.emojione-emoji.core/init!)
  (braid.custom-emoji.core/init!)
  (braid.big-emoji.core/init!)
  (braid.embeds.core/init!)
  (braid.video-embeds.core/init!)
  (braid.image-embeds.core/init!)
  (braid.website-embeds.core/init!))
