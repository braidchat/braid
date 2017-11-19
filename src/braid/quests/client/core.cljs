(ns braid.quests.client.core
  (:require
    [braid.core.api :as api]
    [braid.quests.client.events]
    [braid.quests.client.subs]
    [braid.quests.client.remote-handlers]
    [braid.quests.client.views :as views]
    [braid.quests.client.styles :as styles]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]))

(defn init! []
  (api/dispatch [:braid.core/register-event-listener!
                 :quests
                 (fn [event]
                   (when (not= "quests" (namespace (first event)))
                     (api/dispatch [:quests/update-handler event])))])

  (api/dispatch [:braid.state/register-state!
                 {::quest-records {}}
                 {::quest-records {QuestRecordId QuestRecord}}])

  (api/dispatch [:braid.core/register-header-view!
                 views/quests-header-view])

  (api/dispatch [:braid.core/register-styles!
                 (styles/quests-header)]))
