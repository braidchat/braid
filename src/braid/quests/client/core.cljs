(ns braid.quests.client.core
  (:require
    [re-frame.core :as re-frame]
    [braid.quests.client.events]
    [braid.quests.client.subs]
    [braid.quests.client.helpers :as helpers]
    [braid.quests.client.remote-handlers]
    [braid.core.common.schema :refer [QuestRecordId QuestRecord]]))

(defn initial-data-handler
  [db data]
  (helpers/set-quest-records db (data :quest-records)))

(def event-listener
  (fn [event]
    (when (not= "quests" (namespace (first event)))
      (re-frame/dispatch [:quests/update-handler event]))))

(def initial-state {::quest-records {}})
(def schema {::quest-records {QuestRecordId QuestRecord}})
