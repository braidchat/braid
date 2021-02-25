(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    ; modules:
    [braid.base.core]
    [braid.bots.core]
    [braid.chat.core]
    [braid.disconnect-notice.core]
    [braid.embeds.core]
    [braid.embeds-image.core]
    [braid.embeds-map.core]
    [braid.embeds-video.core]
    [braid.embeds-website.core]
    [braid.embeds-youtube.core]
    [braid.emoji.core]
    [braid.emoji-big.core]
    [braid.emoji-custom.core]
    [braid.emoji-emojione.core]
    [braid.group-create.core]
    [braid.group-explore.core]
    [braid.notices.core]
    [braid.page-inbox.core]
    [braid.page-recent.core]
    [braid.page-subscriptions.core]
    [braid.page-uploads.core]
    [braid.permalinks.core]
    [braid.popovers.core]
    [braid.quests.core]
    [braid.rss.core]
    [braid.search.core]
    [braid.sidebar.core]
    [braid.stars.core]
    [braid.uploads.core]
    [braid.users.core]))

(defn init! [module-fns]
  (hooks/reset-all!)
  (doseq [module-fn module-fns]
    (module-fn)))

(def default
  [;; these expose an api and most modules use them
   braid.base.core/init!
   braid.chat.core/init!
   ;; these expose an api
   braid.popovers.core/init!
   braid.embeds.core/init!
   braid.emoji.core/init!
   ;; rest do not expose api
   braid.bots.core/init!
   braid.disconnect-notice.core/init!
   braid.embeds-image.core/init!
   braid.embeds-map.core/init!
   braid.embeds-video.core/init!
   braid.embeds-website.core/init!
   braid.embeds-youtube.core/init!
   braid.emoji-big.core/init!
   braid.emoji-custom.core/init!
   braid.emoji-emojione.core/init!
   braid.group-create.core/init!
   braid.group-explore.core/init!
   braid.notices.core/init!
   braid.sidebar.core/init!
   braid.page-inbox.core/init!
   braid.page-recent.core/init!
   braid.page-subscriptions.core/init!
   braid.page-uploads.core/init!
   braid.permalinks.core/init!
   braid.quests.core/init!
   braid.rss.core/init!
   braid.stars.core/init!
   braid.search.core/init!
   braid.uploads.core/init!
   braid.users.core/init!])

(defn plus [base to-add]
  (distinct (concat base to-add)))

(defn minus [base to-subtract]
  (let [to-subtract (set to-subtract)]
    (remove to-subtract base)))
