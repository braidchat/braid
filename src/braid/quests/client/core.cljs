(ns braid.quests.client.core
  (:require
    [braid.core.api :as api]
    [braid.quests.client.events]
    [braid.quests.client.subs]
    [braid.quests.client.helpers :as helpers]
    [braid.quests.client.remote-handlers]
    [braid.quests.client.views :as views]
    [braid.quests.client.styles :as styles]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]
    [braid.state.core :refer [register-state!]]
    [braid.core.api :refer [register-event-listener!]]
    [braid.client.core.events :refer [register-initial-user-data-handler!]]
    [braid.client.ui.views.header :refer [register-header-view!]]
    [braid.client.ui.views.styles :refer [register-styles!]]))

(defn init! []
  (register-event-listener!
    :quests
    (fn [event]
      (when (not= "quests" (namespace (first event)))
        (api/dispatch [:quests/update-handler event]))))

  (register-state!
    {::quest-records {}}
    {::quest-records {QuestRecordId QuestRecord}})

  (register-header-view!
    views/quests-header-view)

  (register-styles!
    (styles/quests-header))

  (register-initial-user-data-handler!
    (fn [db data]
      (helpers/set-quest-records db (data :quest-records)))))
