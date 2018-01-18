(ns braid.core.modules
  (:require-macros [braid.core.module-helpers :refer [gen-module-loads]])
  (:require [braid.core.modules-dummy]))

(gen-module-loads)

#_(:require
            [braid.client.ui.views.autocomplete]
            [braid.emoji.core]
            [braid.state.core]
            [braid.quests.client.core]
            [mount.core :refer [defstate]])
#_(defn init! []
  (braid.client.ui.views.autocomplete/init!)
  (braid.emoji.core/init!)
  (braid.state.core/init!))

#_(defstate modules
  :start (init!))
