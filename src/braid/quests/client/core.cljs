(ns braid.quests.client.core
  (:require
    [braid.core.api :as api]
    [braid.quests.client.events]
    [braid.quests.client.subs]
    [braid.quests.client.helpers :as helpers]
    [braid.quests.client.remote-handlers]
    [braid.common.schema :refer [QuestRecordId QuestRecord]]))

(defn initial-data-handler
  [db data]
  (helpers/set-quest-records db (data :quest-records)))

(def event-listener
  [:quests
   (fn [event]
     (when (not= "quests" (namespace (first event)))
       (api/dispatch [:quests/update-handler event])))])

(def state
  [{::quest-records {}}
   {::quest-records {QuestRecordId QuestRecord}}])
