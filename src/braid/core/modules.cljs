(ns braid.core.modules
  (:require
    [braid.client.ui.views.autocomplete]
    [braid.emoji.core]
    [braid.state.core]
    [braid.quests.core]))

(defn init! []
  (braid.client.ui.views.autocomplete/init!)
  (braid.emoji.core/init!)
  (braid.quests.core/init!)
  (braid.state.core/init!))
