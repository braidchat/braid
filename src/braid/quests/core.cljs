(ns braid.quests.core
  (:require
    [braid.core.api :as api]
    [braid.quests.events]
    [braid.quests.subs]
    [braid.quests.remote-handlers]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]))

(api/dispatch [:braid.state/register-state!
               {::quest-records {}}
               {::quest-records {QuestRecordId QuestRecord}}])
