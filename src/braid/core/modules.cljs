(ns braid.core.modules
  (:require
    [braid.client.ui.views.autocomplete]
    [braid.emoji.core]
    [braid.state.core]
    [braid.quests.client.core]))

(defn init! []
  (braid.client.ui.views.autocomplete/init!)
  (braid.emoji.core/init!)
  (braid.quests.client.core/init!)
  (braid.state.core/init!))
