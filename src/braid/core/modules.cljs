(ns braid.core.modules
  (:require
    [braid.client.ui.views.autocomplete]
    [braid.emoji.core]
    [braid.state.core]
    [braid.quests.client.core]
    [mount.core :refer [defstate]]))

(defn init! []
  (braid.client.ui.views.autocomplete/init!)
  (braid.emoji.core/init!)
  (braid.quests.client.core/init!)
  (braid.state.core/init!))

(defstate modules
  :start (init!))
