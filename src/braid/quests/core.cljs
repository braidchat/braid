(ns braid.quests.core
  (:require
    [braid.core.api :as api]
    [braid.quests.events]
    [braid.quests.subs]
    [braid.quests.remote-handlers]
    [braid.quests.views :as views]
    [braid.quests.styles :as styles]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]))

(api/dispatch [:braid.state/register-state!
               {::quest-records {}}
               {::quest-records {QuestRecordId QuestRecord}}])

(api/dispatch [:braid.core/register-header-view!
               views/quests-header-view])

(api/dispatch [:braid.core/register-styles!
               (styles/quests-header)])
