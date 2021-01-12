(ns braid.core.modules
  (:require
    [braid.core.hooks :as hooks]
    ; modules:
    [braid.base.core]
    [braid.big-emoji.core]
    [braid.bots.core]
    [braid.chat.core]
    [braid.custom-emoji.core]
    [braid.disconnect-notice.core]
    [braid.embeds.core]
    [braid.embeds-image.core]
    [braid.embeds-map.core]
    [braid.embeds-video.core]
    [braid.embeds-website.core]
    [braid.embeds-youtube.core]
    [braid.emoji.core]
    [braid.emojione-emoji.core]
    [braid.group-explore.core]
    [braid.notices.core]
    [braid.permalinks.core]
    [braid.popovers.core]
    [braid.quests.core]
    [braid.recent-page.core]
    [braid.rss.core]
    [braid.search.core]
    [braid.sidebar.core]
    [braid.stars.core]
    [braid.subscriptions-page.core]
    [braid.uploads.core]
    [braid.uploads-page.core]
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
   braid.big-emoji.core/init!
   braid.bots.core/init!
   braid.custom-emoji.core/init!
   braid.disconnect-notice.core/init!
   braid.embeds-image.core/init!
   braid.embeds-map.core/init!
   braid.embeds-video.core/init!
   braid.embeds-website.core/init!
   braid.embeds-youtube.core/init!
   braid.emojione-emoji.core/init!
   braid.group-explore.core/init!
   braid.notices.core/init!
   braid.sidebar.core/init!
   braid.permalinks.core/init!
   braid.quests.core/init!
   braid.recent-page.core/init!
   braid.rss.core/init!
   braid.stars.core/init!
   braid.search.core/init!
   braid.subscriptions-page.core/init!
   braid.uploads.core/init!
   braid.uploads-page.core/init!
   braid.users.core/init!])

(defn plus [base to-add]
  (distinct (concat base to-add)))

(defn minus [base to-subtract]
  (let [to-subtract (set to-subtract)]
    (remove to-subtract base)))
