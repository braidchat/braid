(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    ; modules:
    [braid.big-emoji.core]
    [braid.bots.core]
    [braid.core.core]
    [braid.chat.core]
    [braid.custom-emoji.core]
    [braid.disconnect-notice.core]
    [braid.embeds.core]
    [braid.emoji.core]
    [braid.emojione-emoji.core]
    [braid.group-explore.core]
    [braid.image-embeds.core]
    [braid.notices.core]
    [braid.permalinks.core]
    [braid.popovers.core]
    [braid.quests.core]
    [braid.rss.core]
    [braid.search.core]
    [braid.stars.core]
    [braid.uploads.core]
    [braid.uploads-page.core]
    [braid.users.core]
    [braid.video-embeds.core]
    [braid.website-embeds.core]
    [braid.youtube-embeds.core]
    [braid.map-embeds.core]))

(defn init! []
  (hooks/reset-all!)

  (braid.chat.core/init!)
  (braid.big-emoji.core/init!)
  (braid.bots.core/init!)
  (braid.custom-emoji.core/init!)
  (braid.core.core/init!)
  (braid.disconnect-notice.core/init!)
  (braid.embeds.core/init!)
  (braid.emoji.core/init!)
  (braid.emojione-emoji.core/init!)
  (braid.group-explore.core/init!)
  (braid.image-embeds.core/init!)
  (braid.notices.core/init!)
  (braid.permalinks.core/init!)
  (braid.popovers.core/init!)
  (braid.quests.core/init!)
  (braid.rss.core/init!)
  (braid.stars.core/init!)
  (braid.search.core/init!)
  (braid.uploads.core/init!)
  (braid.uploads-page.core/init!)
  (braid.users.core/init!)
  (braid.video-embeds.core/init!)
  (braid.website-embeds.core/init!)
  (braid.youtube-embeds.core/init!)
  (braid.map-embeds.core/init!))
